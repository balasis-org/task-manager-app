package io.github.balasis.taskmanager.context.base.exception.notfound;

public class TaskParticipantNotFoundException extends EntityNotFoundException {
    public TaskParticipantNotFoundException(String message) {
        super(message);
    }
}
