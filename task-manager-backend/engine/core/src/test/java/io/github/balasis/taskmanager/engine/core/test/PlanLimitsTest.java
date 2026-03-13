package io.github.balasis.taskmanager.engine.core.test;

import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;
import io.github.balasis.taskmanager.context.base.limits.PlanLimits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlanLimitsTest {

    private PlanLimits planLimits;

    @BeforeEach
    void setUp() {
        planLimits = new PlanLimits();
    }

    // ── group caps ──────────────────────────────────────────────

    @Test
    void maxGroups_freeIsLowest_teamIsHighest() {
        assertTierOrder(
                groupCap(SubscriptionPlan.FREE),
                groupCap(SubscriptionPlan.STUDENT),
                groupCap(SubscriptionPlan.ORGANIZER),
                groupCap(SubscriptionPlan.TEAM)
        );
    }

    @Test
    void maxGroups_freeGetsTwoGroups() {
        assertEquals(2, groupCap(SubscriptionPlan.FREE));
    }

    // ── member caps ─────────────────────────────────────────────

    @Test
    void maxMembersPerGroup_freeIsLowest_teamIsHighest() {
        assertTierOrder(
                memberCap(SubscriptionPlan.FREE),
                memberCap(SubscriptionPlan.STUDENT),
                memberCap(SubscriptionPlan.ORGANIZER),
                memberCap(SubscriptionPlan.TEAM)
        );
    }

    // ── task caps ───────────────────────────────────────────────

    @Test
    void maxTasksPerGroup_freeIsLowest_teamIsHighest() {
        assertTierOrder(
                taskCap(SubscriptionPlan.FREE),
                taskCap(SubscriptionPlan.STUDENT),
                taskCap(SubscriptionPlan.ORGANIZER),
                taskCap(SubscriptionPlan.TEAM)
        );
    }

    // ── file count caps ─────────────────────────────────────────

    @Test
    void maxCreatorFilesPerTask_freeGetsOneFile() {
        assertEquals(1, planLimits.maxCreatorFilesPerTask(SubscriptionPlan.FREE));
    }

    @Test
    void maxAssigneeFilesPerTask_freeGetsOneFile() {
        assertEquals(1, planLimits.maxAssigneeFilesPerTask(SubscriptionPlan.FREE));
    }

    // ── file size caps ──────────────────────────────────────────

    @Test
    void maxFileSizeBytes_freeGetsFiveMegabytes() {
        long fiveMb = 5L * 1024 * 1024;
        assertEquals(fiveMb, fileSizeCap(SubscriptionPlan.FREE));
    }

    @Test
    void maxFileSizeBytes_paidTiersGetHundredMegabytes() {
        long hundredMb = PlanLimits.HARD_CAP_FILE_SIZE_BYTES;
        assertEquals(hundredMb, fileSizeCap(SubscriptionPlan.STUDENT));
        assertEquals(hundredMb, fileSizeCap(SubscriptionPlan.ORGANIZER));
        assertEquals(hundredMb, fileSizeCap(SubscriptionPlan.TEAM));
    }

    // ── storage budget ──────────────────────────────────────────

    @Test
    void storageBudgetBytes_freeHasNoBudgetTracking() {
        assertEquals(0, planLimits.storageBudgetBytes(SubscriptionPlan.FREE));
    }

    @Test
    void storageBudgetBytes_paidTiersArePositiveAndIncreasing() {
        assertTierOrder(
                0L, // FREE is zero, skip it in ordering
                planLimits.storageBudgetBytes(SubscriptionPlan.STUDENT),
                planLimits.storageBudgetBytes(SubscriptionPlan.ORGANIZER),
                planLimits.storageBudgetBytes(SubscriptionPlan.TEAM)
        );
    }

    // ── download budget ─────────────────────────────────────────

    @Test
    void downloadBudgetBytes_allTiersPositiveAndIncreasing() {
        assertTierOrder(
                planLimits.downloadBudgetBytes(SubscriptionPlan.FREE),
                planLimits.downloadBudgetBytes(SubscriptionPlan.STUDENT),
                planLimits.downloadBudgetBytes(SubscriptionPlan.ORGANIZER),
                planLimits.downloadBudgetBytes(SubscriptionPlan.TEAM)
        );
    }

    // ── email quota ─────────────────────────────────────────────

    @Test
    void emailQuotaPerMonth_freeAndStudentGetZero() {
        assertEquals(0, planLimits.emailQuotaPerMonth(SubscriptionPlan.FREE));
        assertEquals(0, planLimits.emailQuotaPerMonth(SubscriptionPlan.STUDENT));
    }

    @Test
    void emailQuotaPerMonth_organizerGetsLessThanTeam() {
        assertTrue(planLimits.emailQuotaPerMonth(SubscriptionPlan.ORGANIZER)
                < planLimits.emailQuotaPerMonth(SubscriptionPlan.TEAM));
    }

    // ── image scans ─────────────────────────────────────────────

    @Test
    void imageScansPerMonth_freeGetsZero() {
        assertEquals(0, planLimits.imageScansPerMonth(SubscriptionPlan.FREE));
    }

    @Test
    void imageScansPerMonth_paidTiersIncreaseWithTier() {
        assertTierOrder(
                (long) planLimits.imageScansPerMonth(SubscriptionPlan.FREE),
                (long) planLimits.imageScansPerMonth(SubscriptionPlan.STUDENT),
                (long) planLimits.imageScansPerMonth(SubscriptionPlan.ORGANIZER),
                (long) planLimits.imageScansPerMonth(SubscriptionPlan.TEAM)
        );
    }

    // ── isPaid ──────────────────────────────────────────────────

    @Test
    void isPaid_freeReturnsFalse() {
        assertFalse(planLimits.isPaid(SubscriptionPlan.FREE));
    }

    @Test
    void isPaid_allPaidTiersReturnTrue() {
        assertTrue(planLimits.isPaid(SubscriptionPlan.STUDENT));
        assertTrue(planLimits.isPaid(SubscriptionPlan.ORGANIZER));
        assertTrue(planLimits.isPaid(SubscriptionPlan.TEAM));
    }

    // ── download timeout ────────────────────────────────────────

    @Test
    void downloadTimeoutCapMs_freeIsLowest_teamIsHighest() {
        assertTierOrder(
                planLimits.downloadTimeoutCapMs(SubscriptionPlan.FREE),
                planLimits.downloadTimeoutCapMs(SubscriptionPlan.STUDENT),
                planLimits.downloadTimeoutCapMs(SubscriptionPlan.ORGANIZER),
                planLimits.downloadTimeoutCapMs(SubscriptionPlan.TEAM)
        );
    }

    @Test
    void computeDownloadTimeoutMs_tinyFile_clampedToTenSeconds() {
        long result = computeTimeout(100L, SubscriptionPlan.FREE);
        assertEquals(10_000, result, "tiny files should get the 10s floor");
    }

    @Test
    void computeDownloadTimeoutMs_largeFileOnFreeTier_clampedToFreeCap() {
        long hundredMb = 100L * 1024 * 1024;
        long result = computeTimeout(hundredMb, SubscriptionPlan.FREE);
        long freeCap = planLimits.downloadTimeoutCapMs(SubscriptionPlan.FREE);
        assertEquals(freeCap, result, "should be clamped to the free-tier cap");
    }

    @Test
    void computeDownloadTimeoutMs_sameSizeFile_paidTierGetsMoreTime() {
        long fiftyMb = 50L * 1024 * 1024;
        long freeResult = computeTimeout(fiftyMb, SubscriptionPlan.FREE);
        long teamResult = computeTimeout(fiftyMb, SubscriptionPlan.TEAM);
        assertTrue(teamResult >= freeResult,
                "paid tier should get at least as much time as free");
    }

    // ── group creation window ───────────────────────────────────

    @Test
    void maxGroupCreationsPerWindow_freeIsLowest_teamIsHighest() {
        assertTierOrder(
                (long) planLimits.maxGroupCreationsPerWindow(SubscriptionPlan.FREE),
                (long) planLimits.maxGroupCreationsPerWindow(SubscriptionPlan.STUDENT),
                (long) planLimits.maxGroupCreationsPerWindow(SubscriptionPlan.ORGANIZER),
                (long) planLimits.maxGroupCreationsPerWindow(SubscriptionPlan.TEAM)
        );
    }

    // ── private helpers ─────────────────────────────────────────

    private int groupCap(SubscriptionPlan plan) {
        return planLimits.maxGroups(plan);
    }

    private int memberCap(SubscriptionPlan plan) {
        return planLimits.maxMembersPerGroup(plan);
    }

    private int taskCap(SubscriptionPlan plan) {
        return planLimits.maxTasksPerGroup(plan);
    }

    private long fileSizeCap(SubscriptionPlan plan) {
        return planLimits.maxFileSizeBytes(plan);
    }

    private long computeTimeout(long fileSize, SubscriptionPlan plan) {
        return planLimits.computeDownloadTimeoutMs(fileSize, plan);
    }

    private void assertTierOrder(long free, long student, long organizer, long team) {
        assertTrue(free <= student, "FREE should be <= STUDENT");
        assertTrue(student <= organizer, "STUDENT should be <= ORGANIZER");
        assertTrue(organizer <= team, "ORGANIZER should be <= TEAM");
    }
}
