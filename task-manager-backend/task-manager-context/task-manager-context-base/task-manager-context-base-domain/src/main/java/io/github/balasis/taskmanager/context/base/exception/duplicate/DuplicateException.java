package io.github.balasis.taskmanager.context.base.exception.duplicate;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;

public class DuplicateException extends TaskManagerException {
    public DuplicateException(String message) {
        super(message);
    }
}
