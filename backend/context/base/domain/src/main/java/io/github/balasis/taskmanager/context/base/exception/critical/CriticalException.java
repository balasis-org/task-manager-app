package io.github.balasis.taskmanager.context.base.exception.critical;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;

public class CriticalException extends TaskManagerException {
    public CriticalException(String message) {
        super(message);
    }
}
