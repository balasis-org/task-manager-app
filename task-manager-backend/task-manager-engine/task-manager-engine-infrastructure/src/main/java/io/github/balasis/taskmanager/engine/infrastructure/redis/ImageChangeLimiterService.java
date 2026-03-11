package io.github.balasis.taskmanager.engine.infrastructure.redis;

/**
 * Short-term burst limiter for image uploads. Uses Redis to cap how many
 * images a single user can change within a short window (5 per 5 minutes).
 * Fail-open: if Redis is unavailable the global rate limiter (fail-closed)
 * still protects the system.
 */
public interface ImageChangeLimiterService {

    /**
     * Checks whether the user has exceeded the image-change burst limit.
     * Throws {@link io.github.balasis.taskmanager.context.base.exception.business.BusinessRuleException}
     * if the limit is exceeded.
     */
    void checkBurstLimit(long userId);
}
