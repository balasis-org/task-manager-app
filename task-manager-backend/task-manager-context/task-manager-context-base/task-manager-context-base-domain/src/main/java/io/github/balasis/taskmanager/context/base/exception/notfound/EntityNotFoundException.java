package io.github.balasis.taskmanager.context.base.exception.notfound;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;

public class EntityNotFoundException extends TaskManagerException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
