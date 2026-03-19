package io.github.balasis.taskmanager.engine.infrastructure.redis.service;

import io.github.balasis.taskmanager.engine.infrastructure.redis.CommentAnalysisLockService;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

// distributed lock for the comment analysis drainer using Redis SET NX EX.
//
// SET NX EX = "SET if Not eXists, with EXpiry". this is Redis's built-in way to
// implement a distributed lock without Lua scripts or Redlock:
//   SET key value NX EX <seconds>
// returns "OK" if the key didn't exist (lock acquired), null if it did (someone else holds it).
// the EX TTL is a safety net — if the holder crashes, the lock auto-releases after TTL seconds
// so no manual cleanup is needed.
//
// this is a *single-instance* Redis lock (not Redlock). adequate here because we use
// a single Azure Cache for Redis instance, and the worst case of double-processing
// is benign (CAS on the DB row prevents duplicate work).
public class RedisCommentAnalysisLockService implements CommentAnalysisLockService {

    private static final byte[] LOCK_KEY = "comment:analysis:lock".getBytes(StandardCharsets.UTF_8);
    private static final byte[] LOCK_VALUE = "1".getBytes(StandardCharsets.UTF_8);

    private final StatefulRedisConnection<byte[], byte[]> connection;

    public RedisCommentAnalysisLockService(StatefulRedisConnection<byte[], byte[]> connection) {
        this.connection = connection;
    }

    @Override
    public boolean tryAcquireLock(Duration ttl) {
        // SetArgs.Builder.nx() = only set if key doesn't exist (NX flag).
        // .ex() = set an expiry in seconds (EX flag). combined in one atomic command.
        String result = connection.sync().set(
                LOCK_KEY, LOCK_VALUE,
                SetArgs.Builder.nx().ex(ttl.toSeconds()));
        return "OK".equals(result);
    }

    @Override
    public void releaseLock() {
        // DEL unconditionally removes the key. in a more paranoid setup you'd check
        // that the value matches (Lua CAS), but here only the holder calls release.
        connection.sync().del(LOCK_KEY);
    }
}
