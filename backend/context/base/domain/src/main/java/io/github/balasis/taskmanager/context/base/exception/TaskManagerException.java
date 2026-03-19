package io.github.balasis.taskmanager.context.base.exception;

// root of the app exception hierarchy. everything thrown intentionally extends this.
// GlobalExceptionHandler maps each subclass to a specific HTTP status code.
// use RuntimeException (unchecked) so we dont pollute method signatures with throws.
public class TaskManagerException extends RuntimeException {
    public TaskManagerException(String message) {
        super(message);
    }
}
