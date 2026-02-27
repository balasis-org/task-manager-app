package io.github.balasis.taskmanager.context.base.exception.ratelimit;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;

public class ServiceOverloadedException extends TaskManagerException {

    public ServiceOverloadedException(String message) {
        super(message);
    }
}
