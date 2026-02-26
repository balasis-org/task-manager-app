package io.github.balasis.taskmanager.context.base.exception.ratelimit;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;

/**
 * Thrown when the rate-limiting backend (Redis) is unavailable or out of
 * memory.  Signals that no new requests can be accepted until the
 * infrastructure recovers.
 */
public class ServiceOverloadedException extends TaskManagerException {

    public ServiceOverloadedException(String message) {
        super(message);
    }
}
