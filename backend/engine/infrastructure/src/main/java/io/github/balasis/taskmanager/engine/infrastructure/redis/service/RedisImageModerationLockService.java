package io.github.balasis.taskmanager.engine.infrastructure.redis.service;

import io.github.balasis.taskmanager.engine.infrastructure.redis.ImageModerationLockService;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class RedisImageModerationLockService implements ImageModerationLockService {

    private static final byte[] LOCK_KEY = "image:moderation:lock".getBytes(StandardCharsets.UTF_8);
    private static final byte[] LOCK_VALUE = "1".getBytes(StandardCharsets.UTF_8);

    private final StatefulRedisConnection<byte[], byte[]> connection;

    public RedisImageModerationLockService(StatefulRedisConnection<byte[], byte[]> connection) {
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
