package io.github.balasis.taskmanager.context.base.exception.auth;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;

// thrown when the JWT is technically valid but the claims dont match our DB.
// for example: the azureKey in the token doesnt match any user, or the
// user's DB record was tampered with. this is a security red flag
// so it gets logged at WARN level and triggers a 401.
public class AuthenticationIntegrityException extends TaskManagerException {
    public AuthenticationIntegrityException(String message) {
        super(message);
    }
}
