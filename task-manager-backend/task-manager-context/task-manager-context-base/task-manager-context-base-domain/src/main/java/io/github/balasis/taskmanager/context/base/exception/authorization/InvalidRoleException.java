package io.github.balasis.taskmanager.context.base.exception.authorization;

public class InvalidRoleException extends UnauthorizedException {
    public InvalidRoleException(String message) {
        super(message);
    }
}
