package io.github.balasis.taskmanager.context.base.exception.validation;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;

public class InvalidFieldValueException extends TaskManagerException {
    public InvalidFieldValueException(String message) {
        super(message);
    }
}
