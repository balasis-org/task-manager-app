package io.github.balasis.taskmanager.context.base.exception.ratelimit;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;

// thrown by the SecurityCostGuardFilter when too many expensive operations
// (like file uploads or AI analysis) are running concurrently.
// maps to 503 Service Unavailable so the frontend shows a retry message.
public class ServiceOverloadedException extends TaskManagerException {

    public ServiceOverloadedException(String message) {
        super(message);
    }
}
