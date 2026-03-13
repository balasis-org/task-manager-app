package io.github.balasis.taskmanager.engine.infrastructure.redis;

public interface RateLimitService {

    void checkRateLimit(String key);
}
