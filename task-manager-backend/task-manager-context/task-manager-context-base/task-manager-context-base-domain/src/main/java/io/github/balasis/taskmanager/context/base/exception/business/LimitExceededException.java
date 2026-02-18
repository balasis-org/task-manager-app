package io.github.balasis.taskmanager.context.base.exception.business;

public class LimitExceededException extends BusinessRuleException {
    public LimitExceededException(String message) {
        super(message);
    }
}
