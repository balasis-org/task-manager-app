package io.github.balasis.taskmanager.engine.infrastructure.redis;

// Short-term burst limiter for image uploads. Redis-backed, caps how many
// images a user can change within a 5-minute window (max 5). Fail-open:
// if Redis is down, the global rate limiter (fail-closed) still protects us.
public interface ImageChangeLimiterService {

    // Throws BusinessRuleException if the user exceeded the burst limit.
    void checkBurstLimit(long userId);
}
