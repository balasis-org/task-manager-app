package io.github.balasis.taskmanager.engine.monitoring.alert;

import io.github.balasis.taskmanager.context.base.exception.critical.CriticalException;
import io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Catches every {@link CriticalException} thrown by the application, logs with exponential
 * backoff, sends a periodic email digest to the admin, and returns 500 to the client.
 *
 * This is a separate @RestControllerAdvice — the GlobalExceptionHandler intentionally
 * has no handler for CriticalException. Spring picks this handler because CriticalException
 * is more specific than TaskManagerException (handled by GlobalExceptionHandler).
 *
 * Critical exception types:
 *   CriticalBlobStorageException    — Azure Blob Storage infrastructure failure
 *   CriticalAuthIntegrityException  — JWT/token integrity breach (forged key, bad signature)
 *   CriticalInfrastructureException — Redis or other infrastructure component failure
 *
 * Logging strategy — exponential backoff per exception type (per instance):
 *   The FIRST occurrence always logs immediately (with full stack trace).
 *   Subsequent occurrences are suppressed for an interval that doubles each time:
 *     1min → 2min → 4min → 8min → 16min → 32min → 64min → 128min → 240min (cap).
 *   When the backoff elapses and a new log is emitted, the entry includes a count of
 *   how many occurrences were suppressed since the last log — so no data is ever lost,
 *   but a broken Redis hammered by 10k requests doesn't produce 10k log lines.
 *   If 40 minutes pass with ZERO occurrences, the backoff resets so the next event
 *   logs immediately again (the incident is likely over).
 *
 * Email strategy — fixed 4-hour cooldown per exception type (per instance):
 *   One email every 4 hours at most, per exception type. The email body includes the
 *   total occurrence count, instance ID, and the original error message + stack trace.
 *   Subject line is constant per type so email clients (Outlook, Gmail) thread them
 *   into a single conversation — one thread per critical category per day.
 *
 * Horizontal scaling:
 *   All counters/timers are per-instance (in-memory ConcurrentHashMap). Each instance
 *   independently tracks and logs. The instance ID (from WEBSITE_INSTANCE_ID on Azure,
 *   or "local" in dev) is included in every log line and email so the admin can see
 *   which instances are affected and correlate across the fleet.
 *
 * Container safety:
 *   Container restarts clear all state. Worst case: one extra immediate log + one extra
 *   email per type per restart — acceptable for an event that should be rare.
 *
 * Activation:
 *   Email requires the ADMIN_EMAIL env var (or admin.email property).
 *   Logging always works regardless of email configuration.
 *   Instance ID is read from WEBSITE_INSTANCE_ID (Azure App Service sets this automatically).
 *   In dev environments where this env var doesn't exist, it falls back to "local".
 */
@RestControllerAdvice
@Order(1)
public class CriticalExceptionAlerter {

    private static final Logger logger = LoggerFactory.getLogger(CriticalExceptionAlerter.class);

    // ── Tuning constants ────────────────────────────────────────────────

    /** Starting log backoff interval — doubles on each suppressed log. */
    private static final Duration INITIAL_LOG_BACKOFF = Duration.ofMinutes(1);

    /** Maximum log backoff — once reached, it stays here until the incident ends. */
    private static final Duration MAX_LOG_BACKOFF = Duration.ofHours(4);

    /** If no occurrences arrive within this window, the backoff resets to INITIAL.
     *  This means a NEW incident (after silence) always logs immediately. */
    private static final Duration BACKOFF_RESET_AFTER_SILENCE = Duration.ofMinutes(40);

    /** Fixed email cooldown — one email per type per 4 hours. */
    private static final Duration EMAIL_COOLDOWN = Duration.ofHours(4);

    private static final int MAX_STACK_TRACE_LENGTH = 2000;

    // ── Instance identity (for multi-instance correlation) ──────────────

    /** Azure App Service injects WEBSITE_INSTANCE_ID automatically.
     *  Falls back to "local" in dev environments where it doesn't exist. */
    private static final String INSTANCE_ID = resolveInstanceId();

    private static String resolveInstanceId() {
        String azureId = System.getenv("WEBSITE_INSTANCE_ID");
        if (azureId != null && !azureId.isBlank()) {
            // Azure IDs are long hex strings — use last 8 chars for readability
            return azureId.length() > 8 ? azureId.substring(azureId.length() - 8) : azureId;
        }
        return "local";
    }

    // ── Per-exception-type tracking ─────────────────────────────────────

    private final ConcurrentHashMap<String, ExceptionTracker> trackers = new ConcurrentHashMap<>();

    private final EmailClient emailClient;
    private final String adminEmail;

    public CriticalExceptionAlerter(
            @Autowired(required = false) EmailClient emailClient,
            @Value("${admin.email:}") String adminEmail) {
        this.emailClient = emailClient;
        this.adminEmail = adminEmail;
    }

    // ── Exception handler ───────────────────────────────────────────────

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
                .body("A critical error occurred — please try again later");
    }

    // ── Exponential backoff logging ─────────────────────────────────────

    private void logWithBackoff(ExceptionTracker tracker, CriticalException ex) {
        String typeName = ex.getClass().getSimpleName();
        Instant now = Instant.now();

        synchronized (tracker) {
            tracker.totalOccurrences++;
            tracker.suppressedSinceLastLog++;
            tracker.lastOccurrenceAt = now;

            // First time ever — always log immediately
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
                        "CRITICAL [{}] instance={}: {} (recurring — backoff reset after {}min silence, total: {})",
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
            // else: still within backoff window — occurrence counted but not logged
        }
    }

    /**
     * Logs with MDC context so OpenTelemetry auto-instrumentation exports structured
     * custom dimensions to Azure Application Insights. KQL queries can then filter:
     *   traces | where customDimensions.criticalType == "CriticalBlobStorageException"
     */
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

    // ── Email with fixed 4-hour cooldown ────────────────────────────────

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
            // Constant subject per type → email clients thread them into one conversation
            String subject = "[MyTeamTasks] Critical: " + ex.getClass().getSimpleName();
            String body = buildAlertBody(ex, tracker.totalOccurrences);
            emailClient.sendEmail(adminEmail, subject, body);
            logger.info("Critical alert email sent for: {} (instance={})", ex.getClass().getName(), INSTANCE_ID);
        } catch (Exception emailFailure) {
            logger.warn("Failed to send critical alert email for {} (instance={}): {}",
                    ex.getClass().getName(), INSTANCE_ID, emailFailure.getMessage());
        }
    }

    // ── Email body ──────────────────────────────────────────────────────

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

    // ── Guard ───────────────────────────────────────────────────────────

    private boolean isEmailActive() {
        return emailClient != null && adminEmail != null && !adminEmail.isBlank();
    }

    // ── Per-type state tracker ──────────────────────────────────────────

    /**
     * Mutable state for one exception type. All fields are guarded by synchronizing
     * on the tracker instance itself — no need for atomics since contention per type
     * is low (one request thread at a time hits the same exception type in practice).
     */
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
