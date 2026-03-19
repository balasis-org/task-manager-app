package io.github.balasis.taskmanager.context.base.limits;

import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// only active in the stress testing environment (prod-arena-stress profile).
// @Primary tells Spring to prefer this bean over the base PlanLimits when both exist.
// effectively removes download, storage, and analysis budget limits so k6 stress tests
// can hammer the system without getting 429s from budget exhaustion.
// also raises member cap so dynamic k6 users can join beyond the seeded 50.
// the profile is only activated via the stress bicepparam deployment.
@Component
@Primary
@Profile("prod-arena-stress")
public class StressPlanLimitsOverride extends PlanLimits {

    @Override
    public int maxMembersPerGroup(SubscriptionPlan plan) {
        return 500;
    }

    @Override
    public long downloadBudgetBytes(SubscriptionPlan plan) {
        return Long.MAX_VALUE;
    }

    @Override
    public long storageBudgetBytes(SubscriptionPlan plan) {
        return Long.MAX_VALUE;
    }

    @Override
    public int taskAnalysisCreditsPerMonth(SubscriptionPlan plan) {
        return Integer.MAX_VALUE;
    }
}
