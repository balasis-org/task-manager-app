package io.github.balasis.taskmanager.context.base.exception.notfound;

public class TaskNotFoundException extends EntityNotFoundException {
    public TaskNotFoundException(String message) {
        super(message);
    }
}
