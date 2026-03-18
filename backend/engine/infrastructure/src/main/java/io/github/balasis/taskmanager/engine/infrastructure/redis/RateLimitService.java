package io.github.balasis.taskmanager.engine.infrastructure.redis;

// per-key rate limiter. the Redis impl is fail-closed (rejects on failure).
public interface RateLimitService {

    void checkRateLimit(String key);
}
