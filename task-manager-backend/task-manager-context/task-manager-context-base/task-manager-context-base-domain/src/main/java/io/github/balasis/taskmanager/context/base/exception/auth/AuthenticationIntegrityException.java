package io.github.balasis.taskmanager.context.base.exception.auth;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;

public class AuthenticationIntegrityException extends TaskManagerException {
    public AuthenticationIntegrityException(String message) {
        super(message);
    }
}
