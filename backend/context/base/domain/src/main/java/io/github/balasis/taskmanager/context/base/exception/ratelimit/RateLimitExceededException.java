package io.github.balasis.taskmanager.context.base.exception.ratelimit;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;
import lombok.Getter;

// thrown by the Redis rate limiter when a user exceeds their request quota.
// carries retryAfterSeconds which gets set as the Retry-After HTTP header
// so the frontend knows how long to back off.
@Getter
public class RateLimitExceededException extends TaskManagerException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
