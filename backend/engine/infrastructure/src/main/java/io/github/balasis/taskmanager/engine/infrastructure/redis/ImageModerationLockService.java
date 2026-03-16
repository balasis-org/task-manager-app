package io.github.balasis.taskmanager.engine.infrastructure.redis;

import java.time.Duration;

public interface ImageModerationLockService {

    boolean tryAcquireLock(Duration ttl);

    void releaseLock();
}
