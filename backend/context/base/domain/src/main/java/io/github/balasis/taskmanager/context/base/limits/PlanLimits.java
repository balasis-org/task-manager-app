package io.github.balasis.taskmanager.context.base.limits;

import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Single source of truth for every plan-related limit.
// Services must not switch on SubscriptionPlan themselves —
// call methods here and get back a number.
//
// Tier pricing (informational, not enforced here):
// FREE = €0, STUDENT = €1.90/mo, ORGANIZER = €6.20/mo, TEAM = €10/mo, TEAMS_PRO = €20/mo
@Component
public class PlanLimits {

    // Absolute ceiling on file size — no plan, override, or admin can exceed this.
    // Protects against excessive per-request resource consumption
    // (downloads are proxied, not SAS-redirected).
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
            case TEAMS_PRO -> 15;
        };
    }

    // ── member caps (per group) ──────────────────────────────────

    public int maxMembersPerGroup(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 8;
            case STUDENT   -> 20;
            case ORGANIZER -> 30;
            case TEAM      -> 50;
            case TEAMS_PRO -> 50;
        };
    }

    // ── task caps ───────────────────────────────────────────────

    public int maxTasksPerGroup(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 30;
            case STUDENT   -> 100;
            case ORGANIZER -> 300;
            case TEAM      -> 500;
            case TEAMS_PRO -> 500;
        };
    }

    // ── file-count caps (per task) ──────────────────────────────

    public int maxCreatorFilesPerTask(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 1;
            case STUDENT   -> 5;
            case ORGANIZER -> 8;
            case TEAM      -> 8;
            case TEAMS_PRO -> 10;
        };
    }

    public int maxAssigneeFilesPerTask(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 2;
            case STUDENT   -> 5;
            case ORGANIZER -> 8;
            case TEAM      -> 8;
            case TEAMS_PRO -> 10;
        };
    }

    // ── per-file size cap ───────────────────────────────────────

    // Plan-level default for the max single-file upload size in bytes.
    // All paid tiers share the hard-cap; real differentiation between tiers
    // is the total storage budget, not per-file size.
    public long maxFileSizeBytes(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 5L * 1024 * 1024;          //   5 MB
            case STUDENT   -> HARD_CAP_FILE_SIZE_BYTES;   // 100 MB
            case ORGANIZER -> HARD_CAP_FILE_SIZE_BYTES;   // 100 MB
            case TEAM      -> HARD_CAP_FILE_SIZE_BYTES;   // 100 MB
            case TEAMS_PRO -> HARD_CAP_FILE_SIZE_BYTES;   // 100 MB
        };
    }

    // ── storage budget (bytes) ────────────────────────────────

    // Total bytes the group owner may store across all their groups.
    // FREE gets 100 MB (enough for normal use but below STUDENT at 500 MB),
    // keeping the tier ladder monotonic. Per-file and per-task limits
    // further constrain individual uploads.
    public long storageBudgetBytes(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 100L * 1024 * 1024;          // 100 MB
            case STUDENT   -> 500L  * 1024 * 1024;       // 500 MB
            case ORGANIZER -> 2L    * 1024 * 1024 * 1024; //   2 GB
            case TEAM      -> 5L    * 1024 * 1024 * 1024; //   5 GB
            case TEAMS_PRO -> 5L    * 1024 * 1024 * 1024; //   5 GB
        };
    }

    // ── monthly download budget (bytes, 0 = no budget tracking) ─

    // Monthly download budget in bytes. Tracked on the owner, charged when
    // any group member downloads a file. Reset monthly by maintenance.
    public long downloadBudgetBytes(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 500L  * 1024 * 1024;        //  500 MB
            case STUDENT   -> 4L    * 1024 * 1024 * 1024; //    4 GB
            case ORGANIZER -> 25L   * 1024 * 1024 * 1024; //   25 GB
            case TEAM      -> 50L   * 1024 * 1024 * 1024; //   50 GB
            case TEAMS_PRO -> 50L   * 1024 * 1024 * 1024; //   50 GB
        };
    }

    // ── monthly email quota (0 = emails disabled for tier) ──────

    // Max outbound emails per month for groups this user owns.
    // FREE and STUDENT get 0 (email is an Organizer+ feature).
    // When quota is exhausted, emails silently stop; in-app notifications
    // keep working regardless.
    public int emailQuotaPerMonth(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 0;
            case STUDENT   -> 0;
            case ORGANIZER -> 150;
            case TEAM      -> 10_000;
            case TEAMS_PRO -> 10_000;
        };
    }

    // ── group-creation window cap ───────────────────────────────

    // Max groups a user can create between maintenance resets.
    // Prevents delete-and-recreate abuse that piles up orphan blobs.
    public int maxGroupCreationsPerWindow(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 4;
            case STUDENT   -> 12;
            case ORGANIZER -> 20;
            case TEAM      -> 30;
            case TEAMS_PRO -> 30;
        };
    }

    // ── download timeout tiers ──────────────────────────────────

    // Per-tier caps on download duration. Keeps free-tier tight to limit
    // abuse surface; paying users get progressively more headroom.
    public long downloadTimeoutCapMs(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      ->  30_000;  //  30 s — plenty for 5 MB max
            case STUDENT   ->  60_000;  //  1 min
            case ORGANIZER ->  90_000;  //  1.5 min
            case TEAM      -> 120_000;  //  2 min
            case TEAMS_PRO -> 120_000;  //  2 min
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

    // True for any paid tier
    public boolean isPaid(SubscriptionPlan plan) {
        return plan != SubscriptionPlan.FREE;
    }

    // ── monthly image-scan budget ───────────────────────────────

    // Max Content Safety scans per month for image uploads.
    // FREE tier can't upload custom images (0 scans).
    // Caps are generous — the burst limiter handles short-term abuse;
    // this is a longer-term safety net.
    public int imageScansPerMonth(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 0;
            case STUDENT   -> 50;
            case ORGANIZER -> 100;
            case TEAM      -> 150;
            case TEAMS_PRO -> 150;
        };
    }

    // ── monthly task-analysis credit budget ──────────────────────

    public int taskAnalysisCreditsPerMonth(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE      -> 0;
            case STUDENT   -> 0;
            case ORGANIZER -> 0;
            case TEAM      -> 0;
            case TEAMS_PRO -> 8_000;
        };
    }

    // ── helpers: pro-tier check ─────────────────────────────────

    public boolean isPaidPro(SubscriptionPlan plan) {
        return plan == SubscriptionPlan.TEAMS_PRO;
    }
}
