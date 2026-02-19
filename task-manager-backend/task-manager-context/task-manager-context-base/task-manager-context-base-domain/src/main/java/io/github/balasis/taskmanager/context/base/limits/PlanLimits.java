package io.github.balasis.taskmanager.context.base.limits;

import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;

/**
 * Centralised limit configuration per subscription plan.
 */
public final class PlanLimits {

    private PlanLimits() { /* utility */ }

    /* ── application-wide cap (plan-independent) ── */
    public static final int MAX_USERS = 100;

    /* ── per-plan limits ── */

    public static int maxGroups(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE    -> 3;
            case PREMIUM -> 10;
        };
    }

    public static int maxTasksPerGroup(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE    -> 50;
            case PREMIUM -> 500;
        };
    }
}
