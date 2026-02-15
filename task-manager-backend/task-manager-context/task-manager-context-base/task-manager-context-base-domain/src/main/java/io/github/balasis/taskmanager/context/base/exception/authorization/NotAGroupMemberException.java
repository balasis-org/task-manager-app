package io.github.balasis.taskmanager.context.base.exception.authorization;

public class NotAGroupMemberException extends UnauthorizedException {
    public NotAGroupMemberException(String message) {
        super(message);
    }
}
