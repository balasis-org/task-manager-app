package io.github.balasis.taskmanager.context.base.exception.notfound;

public class UserNotFoundException extends EntityNotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
