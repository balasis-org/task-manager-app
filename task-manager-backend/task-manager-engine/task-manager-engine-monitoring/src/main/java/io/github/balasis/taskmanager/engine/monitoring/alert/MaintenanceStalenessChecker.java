package io.github.balasis.taskmanager.engine.monitoring.alert;

import io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

/**
 * Periodically checks whether the full maintenance job has run recently.
 * If the maintenance status row shows the job is overdue (nextResetAt is
 * far in the past), this checker logs a CRITICAL-level alert with MDC
 * fields (picked up by App Insights) and sends an email to the admin.
 *
 * <p>This is NOT a HealthIndicator on purpose: a stale maintenance job
 * should not make /health report DOWN and trigger container restarts.
 * The app still functions; the only impact is that group-creation
 * counters stop resetting and orphan blobs accumulate.</p>
 */
@Component
public class MaintenanceStalenessChecker {

    private static final Logger logger = LoggerFactory.getLogger(MaintenanceStalenessChecker.class);

    /** Alert if nextResetAt is more than 12 hours in the past. */
    private static final Duration STALENESS_THRESHOLD = Duration.ofHours(12);

    /** Don't spam emails — wait at least 6 h between sends. */
    private static final Duration EMAIL_COOLDOWN = Duration.ofHours(6);

    private static final String INSTANCE_ID = resolveInstanceId();

    private final DataSource dataSource;
    private final EmailClient emailClient;
    private final String adminEmail;

    private volatile Instant lastEmailAt;

    public MaintenanceStalenessChecker(
            DataSource dataSource,
            @Autowired(required = false) EmailClient emailClient,
            @Value("${admin.email:}") String adminEmail) {
        this.dataSource = dataSource;
        this.emailClient = emailClient;
        this.adminEmail = adminEmail;
    }

    /**
     * Runs every hour, starting 5 minutes after boot.
     * Reads the singleton MaintenanceStatus row (id = 1) and checks
     * whether {@code nextResetAt} is dangerously far in the past.
     */
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 300_000)
    public void checkStaleness() {
        try {
            Instant nextResetAt = readNextResetAt();

            if (nextResetAt == null) {
                // Row doesn't exist or nextResetAt is null: first full
                // maintenance hasn't run yet (fresh deployment). No alert.
                return;
            }

            Duration overdue = Duration.between(nextResetAt, Instant.now());
            if (overdue.compareTo(STALENESS_THRESHOLD) > 0) {
                String message = String.format(
                        "Maintenance job has not run on schedule. " +
                        "Expected reset at %s, now %d h overdue. " +
                        "Group-creation counters are NOT being reset and " +
                        "orphan blobs may be accumulating. " +
                        "Check Azure Container Instance logs.",
                        nextResetAt, overdue.toHours());

                logStale(message);
                sendEmailIfCooldownElapsed(message);
            }
        } catch (Exception e) {
            // Never let the checker crash the scheduler thread.
            logger.warn("MaintenanceStalenessChecker failed to read status: {}", e.getMessage());
        }
    }

    private Instant readNextResetAt() throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT nextResetAt FROM MaintenanceStatus WHERE id = 1")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("nextResetAt");
                return ts != null ? ts.toInstant() : null;
            }
            return null;
        }
    }

    private void logStale(String message) {
        MDC.put("criticalType", "MaintenanceStale");
        MDC.put("instanceId", INSTANCE_ID);
        try {
            logger.error("CRITICAL [MaintenanceStale] instance={}: {}", INSTANCE_ID, message);
        } finally {
            MDC.remove("criticalType");
            MDC.remove("instanceId");
        }
    }

    private void sendEmailIfCooldownElapsed(String message) {
        if (emailClient == null || adminEmail == null || adminEmail.isBlank()) return;

        Instant now = Instant.now();
        if (lastEmailAt != null && Duration.between(lastEmailAt, now).compareTo(EMAIL_COOLDOWN) < 0) {
            return;
        }
        lastEmailAt = now;

        try {
            emailClient.sendEmail(adminEmail,
                    "[MyTeamTasks] Critical: Maintenance job stale",
                    "The maintenance job has not completed successfully within the expected schedule.\n\n" +
                    "Instance : " + INSTANCE_ID + "\n" +
                    "Details  : " + message + "\n\n" +
                    "Action required: Check Azure Container Instance logs for the maintenance job.\n" +
                    "Email cooldown : " + EMAIL_COOLDOWN.toHours() + " hours (next email for this alert suppressed)\n");
            logger.info("Maintenance staleness alert email sent (instance={})", INSTANCE_ID);
        } catch (Exception e) {
            logger.warn("Failed to send maintenance staleness alert email: {}", e.getMessage());
        }
    }

    private static String resolveInstanceId() {
        String azureId = System.getenv("WEBSITE_INSTANCE_ID");
        if (azureId != null && !azureId.isBlank()) {
            return azureId.length() > 8 ? azureId.substring(azureId.length() - 8) : azureId;
        }
        return "local";
    }
}
