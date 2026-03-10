package io.github.balasis.taskmanager.context.base.limits;

import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for every plan-related limit.
 * Services MUST NOT switch on {@link SubscriptionPlan} themselves —
 * they call methods here and get back a number.
 *
 * <p>Tier pricing (informational, not enforced here):
 * FREE = $0, STUDENT = $1.90/mo, ORGANIZER = $6.20/mo, TEAM = $10/mo
 */
@Component
public class PlanLimits {

    /**
     * Absolute file-size ceiling that no plan, override, or admin can exceed.
     * Protects against excessive per-request resource consumption on the app
     * server (downloads are proxied, not SAS-redirected).
     */
    public static final long HARD_CAP_FILE_SIZE_BYTES = 100L * 1024 * 1024; // 100 MB

    @Value("${app.max-users:10000}")
    private int maxUsers;

    public int getMaxUsers() {
        return maxUsers;
    }

    // ── group caps ──────────────────────────────────────────────

    public int maxGroups(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 2;
            case STUDENT   -> 5;
            case ORGANIZER -> 10;
            case TEAM      -> 15;
        };
    }

    // ── member caps (per group) ──────────────────────────────────

    public int maxMembersPerGroup(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 8;
            case STUDENT   -> 20;
            case ORGANIZER -> 30;
            case TEAM      -> 50;
        };
    }

    // ── task caps ───────────────────────────────────────────────

    public int maxTasksPerGroup(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 30;
            case STUDENT   -> 100;
            case ORGANIZER -> 300;
            case TEAM      -> 500;
        };
    }

    // ── file-count caps (per task) ──────────────────────────────

    public int maxCreatorFilesPerTask(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 1;
            case STUDENT   -> 3;
            case ORGANIZER -> 5;
            case TEAM      -> 5;
        };
    }

    public int maxAssigneeFilesPerTask(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 1;
            case STUDENT   -> 3;
            case ORGANIZER -> 5;
            case TEAM      -> 5;
        };
    }

    // ── per-file size cap ───────────────────────────────────────

    /**
     * Plan-level default for the maximum single-file upload size (bytes).
     * All paid tiers share the hard-cap value; the real differentiation
     * between tiers is the total storage budget, not per-file size.
     */
    public long maxFileSizeBytes(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 5L * 1024 * 1024;          //   5 MB
            case STUDENT   -> HARD_CAP_FILE_SIZE_BYTES;   // 100 MB
            case ORGANIZER -> HARD_CAP_FILE_SIZE_BYTES;   // 100 MB
            case TEAM      -> HARD_CAP_FILE_SIZE_BYTES;   // 100 MB
        };
    }

    // ── storage budget (bytes, 0 = no budget tracking) ──────────

    /**
     * Total bytes the group-leader may store across every group they own.
     * FREE tier returns 0 — no budget tracking; only hard per-task limits.
     */
    public long storageBudgetBytes(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 0;
            case STUDENT   -> 500L  * 1024 * 1024;       // 500 MB
            case ORGANIZER -> 2L    * 1024 * 1024 * 1024; //   2 GB
            case TEAM      -> 5L    * 1024 * 1024 * 1024; //   5 GB
        };
    }

    // ── monthly download budget (bytes, 0 = no budget tracking) ─

    /**
     * Total bytes that may be downloaded from groups this user owns
     * per calendar month. Tracked on the owner, charged when any
     * member downloads a file. Reset monthly by maintenance.
     */
    public long downloadBudgetBytes(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 500L  * 1024 * 1024;        //  500 MB
            case STUDENT   -> 4L    * 1024 * 1024 * 1024; //    4 GB
            case ORGANIZER -> 25L   * 1024 * 1024 * 1024; //   25 GB
            case TEAM      -> 50L   * 1024 * 1024 * 1024; //   50 GB
        };
    }

    // ── monthly email quota (0 = emails disabled for tier) ──────

    /**
     * Maximum outbound emails per month for groups this user owns.
     * FREE and STUDENT get 0 — email notifications are an Organizer+ feature.
     * Organizer gets a low taster quota; Team gets the full bucket.
     * When the quota is exhausted, emails silently stop; in-app notifications
     * continue working regardless.
     */
    public int emailQuotaPerMonth(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 0;
            case STUDENT   -> 0;
            case ORGANIZER -> 150;
            case TEAM      -> 10_000;
        };
    }

    // ── group-creation window cap ───────────────────────────────

    /**
     * How many groups a user may create between maintenance resets.
     * Prevents delete-and-recreate abuse that piles up orphan blobs.
     */
    public int maxGroupCreationsPerWindow(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 4;
            case STUDENT   -> 12;
            case ORGANIZER -> 20;
            case TEAM      -> 30;
        };
    }

    // ── download timeout tiers ──────────────────────────────────

    /**
     * Hard per-tier caps on download duration. Keeps free-tier tight to
     * limit abuse surface, and gives paying users progressively more
     * headroom for large files on slower connections.
     */
    public long downloadTimeoutCapMs(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      ->  30_000;  //  30 s — plenty for 5 MB max
            case STUDENT   ->  60_000;  //  1 min
            case ORGANIZER ->  90_000;  //  1.5 min
            case TEAM      -> 120_000;  //  2 min
        };
    }

    // 5 s base (blob RTT + TCP ramp) + transfer at assumed 2 MB/s floor, clamped to [10 s .. tier cap]
    public long computeDownloadTimeoutMs(long fileSizeBytes, SubscriptionPlan downloaderPlan) {
        long minSpeedBytesPerSec = 2048L * 1024; // 2 MB/s — see ToS minimum-speed clause
        long transferMs = (fileSizeBytes * 1000) / minSpeedBytesPerSec;
        long unclamped = 5_000 + transferMs;
        long cap = downloadTimeoutCapMs(downloaderPlan);
        return Math.max(10_000, Math.min(cap, unclamped));
    }

    // ── helpers: plan-awareness check ───────────────────────────

    /** Returns {@code true} for any paid tier. */
    public boolean isPaid(SubscriptionPlan plan) {
        return plan != SubscriptionPlan.FREE;
    }

    // ── monthly image-scan budget ───────────────────────────────

    /**
     * Maximum Content Safety scans per month for image uploads.
     * FREE tier cannot upload custom images (0 scans).
     * Caps are intentionally generous — the burst limiter handles
     * short-term abuse; this is a long-term safety net.
     */
    public int imageScansPerMonth(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 0;
            case STUDENT   -> 50;
            case ORGANIZER -> 100;
            case TEAM      -> 150;
        };
    }
}
