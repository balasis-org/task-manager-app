package io.github.balasis.taskmanager.context.base.exception.ratelimit;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;
import lombok.Getter;

@Getter
public class RateLimitExceededException extends TaskManagerException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
