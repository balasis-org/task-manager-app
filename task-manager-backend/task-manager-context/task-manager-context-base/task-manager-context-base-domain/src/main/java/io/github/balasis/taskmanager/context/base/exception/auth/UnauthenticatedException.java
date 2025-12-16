package io.github.balasis.taskmanager.context.base.exception.auth;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;

public class UnauthenticatedException extends TaskManagerException {
    public UnauthenticatedException(String message) {
        super(message);
    }
}
