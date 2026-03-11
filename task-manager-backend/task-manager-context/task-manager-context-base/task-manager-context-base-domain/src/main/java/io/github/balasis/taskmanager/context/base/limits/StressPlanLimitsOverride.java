package io.github.balasis.taskmanager.context.base.limits;

import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Primary
@Profile("prod-arena-stress")
public class StressPlanLimitsOverride extends PlanLimits {

    @Override
    public long downloadBudgetBytes(SubscriptionPlan plan) {
        return Long.MAX_VALUE;
    }

    @Override
    public long storageBudgetBytes(SubscriptionPlan plan) {
        return Long.MAX_VALUE;
    }
}
