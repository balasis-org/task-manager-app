package io.github.balasis.taskmanager.context.base.enumeration;

// 5-tier pricing ladder. each tier unlocks progressively higher limits
// (groups, members, storage, downloads, email, AI analysis).
// the actual numeric limits live in PlanLimits, not here.
// FREE = no payment, STUDENT = 1.90/mo, ORGANIZER = 6.20/mo,
// TEAM = 10/mo, TEAMS_PRO = 20/mo (only tier with AI analysis)
public enum SubscriptionPlan {
    FREE,
    STUDENT,
    ORGANIZER,
    TEAM,
    TEAMS_PRO
}
