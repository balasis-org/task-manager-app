package io.github.balasis.taskmanager.context.base.exception.business;

// thrown when a user tries to exceed a plan limit (max groups, max files, storage budget, etc.)
// the frontend catches the 409 and shows the TierUpgradePopup to upsell.
public class LimitExceededException extends BusinessRuleException {
    public LimitExceededException(String message) {
        super(message);
    }
}
