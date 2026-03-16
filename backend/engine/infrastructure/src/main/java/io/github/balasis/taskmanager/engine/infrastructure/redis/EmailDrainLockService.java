package io.github.balasis.taskmanager.engine.infrastructure.redis;

import java.time.Duration;

/**
 * Distributed lock for the email queue drainer.
 * Ensures only one application instance drains the outbox at a time,
 * preventing duplicate sends and ACS rate-limit violations.
 */
public interface EmailDrainLockService {

    boolean tryAcquireLock(Duration ttl);

    void releaseLock();
}
