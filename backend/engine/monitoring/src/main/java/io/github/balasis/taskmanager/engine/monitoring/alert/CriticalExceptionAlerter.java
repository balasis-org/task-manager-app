package io.github.balasis.taskmanager.engine.monitoring.alert;

import io.github.balasis.taskmanager.context.base.exception.critical.CriticalException;
import io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

// Catches CriticalException subclasses, logs them with exponential backoff
// (so a broken Redis doesn't spam 10k log lines) and sends an email digest
// every 4h to the admin. Returns 500 to the client.
// Separate from GlobalExceptionHandler because Spring matches CriticalException
// more specifically than the generic TaskManagerException handler.
@RestControllerAdvice
@Order(1)
public class CriticalExceptionAlerter {

    private static final Logger logger = LoggerFactory.getLogger(CriticalExceptionAlerter.class);

    // backoff / cooldown tuning
    private static final Duration INITIAL_LOG_BACKOFF = Duration.ofMinutes(1);  // doubles each time
    private static final Duration MAX_LOG_BACKOFF = Duration.ofHours(4);
    private static final Duration BACKOFF_RESET_AFTER_SILENCE = Duration.ofMinutes(40); // resets if quiet
    private static final Duration EMAIL_COOLDOWN = Duration.ofHours(4);

    private static final int MAX_STACK_TRACE_LENGTH = 2000;

    // WEBSITE_INSTANCE_ID is set by Azure App Service; fallback to "local" in dev
    private static final String INSTANCE_ID = resolveInstanceId();

    private static String resolveInstanceId() {
        String azureId = System.getenv("WEBSITE_INSTANCE_ID");
        if (azureId != null && !azureId.isBlank()) {
            return azureId.length() > 8 ? azureId.substring(azureId.length() - 8) : azureId;
        }
        return "local";
    }

    private final ConcurrentHashMap<String, ExceptionTracker> trackers = new ConcurrentHashMap<>();

    private final EmailClient emailClient;
    private final String adminEmail;

    public CriticalExceptionAlerter(
            @Autowired(required = false) @Qualifier("adminEmailClient") EmailClient emailClient,
            @Value("${admin.email:}") String adminEmail) {
        this.emailClient = emailClient;
        this.adminEmail = adminEmail;
    }

    @ExceptionHandler(CriticalException.class)
    public ResponseEntity<String> handleCriticalException(CriticalException ex) {
        String exceptionKey = ex.getClass().getName();
        ExceptionTracker tracker = trackers.computeIfAbsent(exceptionKey, k -> new ExceptionTracker());

        logWithBackoff(tracker, ex);

        if (isEmailActive()) {
            sendEmailIfCooldownElapsed(tracker, ex);
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("A critical error occurred. Please try again later");
    }

    private void logWithBackoff(ExceptionTracker tracker, CriticalException ex) {
        String typeName = ex.getClass().getSimpleName();
        Instant now = Instant.now();

        synchronized (tracker) {
            tracker.totalOccurrences++;
            tracker.suppressedSinceLastLog++;
            tracker.lastOccurrenceAt = now;

            // first occurrence - log right away
            if (tracker.lastLogAt == null) {
                tracker.lastLogAt = now;
                tracker.suppressedSinceLastLog = 0;
                tracker.currentLogBackoff = INITIAL_LOG_BACKOFF;
                logCritical(typeName, tracker,
                        "CRITICAL [{}] instance={}: {} (first occurrence)",
                        typeName, INSTANCE_ID, ex.getMessage(), ex);
                return;
            }

            // If the incident went silent long enough, reset backoff so next event logs fresh
            Duration silenceGap = Duration.between(tracker.lastLogAt, now);
            if (tracker.suppressedSinceLastLog == 1 && silenceGap.compareTo(BACKOFF_RESET_AFTER_SILENCE) >= 0) {
                tracker.lastLogAt = now;
                tracker.suppressedSinceLastLog = 0;
                tracker.currentLogBackoff = INITIAL_LOG_BACKOFF;
                logCritical(typeName, tracker,
                        "CRITICAL [{}] instance={}: {} (recurring - backoff reset after {}min silence, total: {})",
                        typeName, INSTANCE_ID, ex.getMessage(),
                        silenceGap.toMinutes(), tracker.totalOccurrences, ex);
                return;
            }

            // Check if the current backoff interval has elapsed
            Duration sinceLastLog = Duration.between(tracker.lastLogAt, now);
            if (sinceLastLog.compareTo(tracker.currentLogBackoff) >= 0) {
                long suppressed = tracker.suppressedSinceLastLog;
                tracker.lastLogAt = now;
                tracker.suppressedSinceLastLog = 0;

                // Double the backoff for next time, capped at MAX
                tracker.currentLogBackoff = tracker.currentLogBackoff.multipliedBy(2);
                if (tracker.currentLogBackoff.compareTo(MAX_LOG_BACKOFF) > 0) {
                    tracker.currentLogBackoff = MAX_LOG_BACKOFF;
                }

                logCritical(typeName, tracker,
                        "CRITICAL [{}] instance={}: {} ({} suppressed since last log, total: {}, next log in ~{}min)",
                        typeName, INSTANCE_ID, ex.getMessage(),
                        suppressed, tracker.totalOccurrences,
                        tracker.currentLogBackoff.toMinutes(), ex);
            }
            // else: still within backoff window, occurrence counted but not logged
        }
    }

    // sets MDC fields so OTel exports them as custom dimensions in App Insights
    private void logCritical(String typeName, ExceptionTracker tracker,
                             String format, Object... args) {
        MDC.put("criticalType", typeName);
        MDC.put("instanceId", INSTANCE_ID);
        MDC.put("totalOccurrences", String.valueOf(tracker.totalOccurrences));
        try {
            logger.error(format, args);
        } finally {
            MDC.remove("criticalType");
            MDC.remove("instanceId");
            MDC.remove("totalOccurrences");
        }
    }

    private void sendEmailIfCooldownElapsed(ExceptionTracker tracker, CriticalException ex) {
        Instant now = Instant.now();
        boolean shouldSend;

        synchronized (tracker) {
            if (tracker.lastEmailAt == null ||
                    Duration.between(tracker.lastEmailAt, now).compareTo(EMAIL_COOLDOWN) >= 0) {
                tracker.lastEmailAt = now;
                tracker.occurrencesSinceLastEmail = tracker.totalOccurrences;
                shouldSend = true;
            } else {
                shouldSend = false;
            }
        }

        if (!shouldSend) return;

        try {
            // same subject per type so email clients group them together
            String subject = "[MyTeamTasks] Critical: " + ex.getClass().getSimpleName();
            String body = buildAlertBody(ex, tracker.totalOccurrences);
            emailClient.sendEmail(adminEmail, subject, body);
            logger.info("Critical alert email sent for: {} (instance={})", ex.getClass().getName(), INSTANCE_ID);
        } catch (Exception emailFailure) {
            logger.warn("Failed to send critical alert email for {} (instance={}): {}",
                    ex.getClass().getName(), INSTANCE_ID, emailFailure.getMessage());
        }
    }

    private String buildAlertBody(CriticalException exception, long totalOccurrences) {
        StringBuilder body = new StringBuilder();
        body.append("A critical exception is occurring in the myteamtasks application.\n\n");
        body.append("Instance         : ").append(INSTANCE_ID).append("\n");
        body.append("Exception type   : ").append(exception.getClass().getName()).append("\n");
        body.append("Message          : ").append(exception.getMessage()).append("\n");
        body.append("Total occurrences: ").append(totalOccurrences).append(" (this instance)\n");
        body.append("Email cooldown   : 4 hours (next email for this type suppressed)\n\n");
        body.append("Stack trace:\n");

        for (StackTraceElement frame : exception.getStackTrace()) {
            body.append("  at ").append(frame).append("\n");
            if (body.length() > MAX_STACK_TRACE_LENGTH) {
                body.append("  ... (truncated)\n");
                break;
            }
        }

        return body.toString();
    }

    private boolean isEmailActive() {
        return emailClient != null && adminEmail != null && !adminEmail.isBlank();
    }

    // per-type mutable state, synchronized on the tracker instance
    private static class ExceptionTracker {
        long totalOccurrences;
        long suppressedSinceLastLog;
        long occurrencesSinceLastEmail;
        Instant lastOccurrenceAt;
        Instant lastLogAt;
        Instant lastEmailAt;
        Duration currentLogBackoff = INITIAL_LOG_BACKOFF;
    }
}
