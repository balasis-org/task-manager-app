package io.github.balasis.taskmanager.context.base.exception.authorization;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;

public class UnauthorizedException extends TaskManagerException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
