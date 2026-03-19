package io.github.balasis.taskmanager.context.base.exception.critical;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;

// thrown when something fundamentally wrong happens (blob storage unreachable,
// auth token integrity violation, etc). the CriticalExceptionAlerter aspect
// catches these and fires an admin email alert so we know immediately.
// maps to 500 in GlobalExceptionHandler.
public class CriticalException extends TaskManagerException {
    public CriticalException(String message) {
        super(message);
    }
}
