package io.github.balasis.taskmanager.engine.infrastructure.redis;

import java.time.Duration;

public interface CommentAnalysisLockService {

    boolean tryAcquireLock(Duration ttl);

    void releaseLock();
}
