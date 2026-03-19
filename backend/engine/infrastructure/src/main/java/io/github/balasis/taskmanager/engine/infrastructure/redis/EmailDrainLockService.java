package io.github.balasis.taskmanager.engine.infrastructure.redis;

import java.time.Duration;

// Distributed lock for the email queue drainer.
// Only one app instance should drain the outbox at a time to avoid
// duplicate sends and ACS rate-limit violations.
public interface EmailDrainLockService {

    boolean tryAcquireLock(Duration ttl);

    void releaseLock();
}
