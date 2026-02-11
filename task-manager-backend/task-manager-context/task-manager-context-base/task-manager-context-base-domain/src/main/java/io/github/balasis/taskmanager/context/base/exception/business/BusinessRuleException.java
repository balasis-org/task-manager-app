package io.github.balasis.taskmanager.context.base.exception.business;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;

public class BusinessRuleException extends TaskManagerException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
