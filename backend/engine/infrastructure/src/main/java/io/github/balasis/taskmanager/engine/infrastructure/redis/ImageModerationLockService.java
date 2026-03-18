package io.github.balasis.taskmanager.engine.infrastructure.redis;

import java.time.Duration;

// distributed lock so only one app instance runs the image moderation drainer at a time
public interface ImageModerationLockService {

    boolean tryAcquireLock(Duration ttl);

    void releaseLock();
}
