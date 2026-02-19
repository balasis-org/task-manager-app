package io.github.balasis.taskmanager.engine.infrastructure.redis;

/**
 * Checks per-IP rate limits using a distributed token-bucket backed by Redis.
 * <p>
 * If the rate-limiting backend is unavailable the implementation MUST
 * fail-open (allow the request) rather than blocking users.
 */
public interface RateLimitService {

    /**
     * Consume one token for {@code key}.
     * Throws {@link io.github.balasis.taskmanager.context.base.exception.ratelimit.RateLimitExceededException}
     * if the bucket is exhausted.
     */
    void checkRateLimit(String key);
}
