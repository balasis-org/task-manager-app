package io.github.balasis.taskmanager.engine.infrastructure.redis;

import java.time.Duration;

// distributed lock so only one app instance runs the comment analysis drainer at a time
public interface CommentAnalysisLockService {

    boolean tryAcquireLock(Duration ttl);

    void releaseLock();
}
