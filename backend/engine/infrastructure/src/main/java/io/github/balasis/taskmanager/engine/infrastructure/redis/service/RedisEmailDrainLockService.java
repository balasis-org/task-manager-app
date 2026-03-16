package io.github.balasis.taskmanager.engine.infrastructure.redis.service;

import io.github.balasis.taskmanager.engine.infrastructure.redis.EmailDrainLockService;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class RedisEmailDrainLockService implements EmailDrainLockService {

    private static final byte[] LOCK_KEY = "email:drain:lock".getBytes(StandardCharsets.UTF_8);
    private static final byte[] LOCK_VALUE = "1".getBytes(StandardCharsets.UTF_8);

    private final StatefulRedisConnection<byte[], byte[]> connection;

    public RedisEmailDrainLockService(StatefulRedisConnection<byte[], byte[]> connection) {
        this.connection = connection;
    }

    @Override
    public boolean tryAcquireLock(Duration ttl) {
        String result = connection.sync().set(
                LOCK_KEY, LOCK_VALUE,
                SetArgs.Builder.nx().ex(ttl.toSeconds()));
        return "OK".equals(result);
    }

    @Override
    public void releaseLock() {
        connection.sync().del(LOCK_KEY);
    }
}
