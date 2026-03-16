package io.github.balasis.taskmanager.engine.infrastructure.redis.service;

import io.github.balasis.taskmanager.engine.infrastructure.redis.CommentAnalysisLockService;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class RedisCommentAnalysisLockService implements CommentAnalysisLockService {

    private static final byte[] LOCK_KEY = "comment:analysis:lock".getBytes(StandardCharsets.UTF_8);
    private static final byte[] LOCK_VALUE = "1".getBytes(StandardCharsets.UTF_8);

    private final StatefulRedisConnection<byte[], byte[]> connection;

    public RedisCommentAnalysisLockService(StatefulRedisConnection<byte[], byte[]> connection) {
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
