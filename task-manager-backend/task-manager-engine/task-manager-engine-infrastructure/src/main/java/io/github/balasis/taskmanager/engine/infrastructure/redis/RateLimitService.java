package io.github.balasis.taskmanager.engine.infrastructure.redis;

/**
 * Checks per-user rate limits using a distributed token-bucket backed by Redis.
 * <p>
 * If the rate-limiting backend is unavailable the implementation MUST
 * fail-closed (reject the request) to protect the system under load.
 */
public interface RateLimitService {

    /**
     * Consume one token for {@code key}.
     * Throws {@link io.github.balasis.taskmanager.context.base.exception.ratelimit.RateLimitExceededException}
     * if the bucket is exhausted.
     */
    void checkRateLimit(String key);
}
