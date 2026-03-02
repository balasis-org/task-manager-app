package io.github.balasis.taskmanager.engine.infrastructure.redis.service;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.engine.infrastructure.redis.PresenceService;
import io.lettuce.core.Range;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Redis-backed presence tracker using a sorted set per group.
 *
 * <h3>Data model</h3>
 * <pre>
 *   Key:    presence:{groupId}        (one sorted set per group)
 *   Member: "{userId}"                (the user's database ID as a string)
 *   Score:  epoch-second of the last heartbeat
 * </pre>
 *
 * <h3>How individual users expire</h3>
 * There is no per-member TTL in a sorted set.  Instead, every
 * {@link #getPresent} call first runs {@code ZREMRANGEBYSCORE} to
 * evict entries whose score (= last heartbeat) is older than
 * {@link #HEARTBEAT_TTL_SECONDS}.  The whole key also carries a
 * safety TTL ({@link #KEY_TTL_SECONDS}) so it self-destructs if
 * the group becomes completely inactive.
 *
 * <h3>Redis commands used (total: 4)</h3>
 * <ul>
 *   <li>{@code ZADD}  — upsert heartbeat (write path)</li>
 *   <li>{@code EXPIRE} — reset safety TTL on the key (write path)</li>
 *   <li>{@code ZREMRANGEBYSCORE} — evict stale members (read path)</li>
 *   <li>{@code ZRANGEBYSCORE}    — return active members (read path)</li>
 * </ul>
 *
 * <h3>Memory footprint</h3>
 * Each member in the sorted set costs ~60-70 bytes (element + score +
 * skiplist pointers).  A group with 50 active users ≈ 3.5 KB.
 */
public class RedisPresenceService extends BaseComponent implements PresenceService {

    /** Users not seen within this window are considered offline. */
    private static final int HEARTBEAT_TTL_SECONDS = 90;

    /** Safety TTL on the whole key — auto-deletes if the group goes fully idle. */
    private static final int KEY_TTL_SECONDS = 300;

    private static final String KEY_PREFIX = "presence:";

    private final StatefulRedisConnection<byte[], byte[]> redisConnection;

    public RedisPresenceService(StatefulRedisConnection<byte[], byte[]> redisConnection) {
        this.redisConnection = redisConnection;
    }

    // ── Write path (piggybacked on existing polling requests) ────────

    @Override
    public void heartbeat(Long groupId, Long userId) {
        try {
            RedisCommands<byte[], byte[]> cmd = redisConnection.sync();
            byte[] key    = key(groupId);
            byte[] member = String.valueOf(userId).getBytes(StandardCharsets.UTF_8);
            double score  = Instant.now().getEpochSecond();

            cmd.zadd(key, score, member);
            cmd.expire(key, KEY_TTL_SECONDS);
        } catch (Exception e) {
            // Best-effort — never fail the caller's request.
            logger.warn("Presence heartbeat failed for group {}: {}",
                    groupId, e.getMessage() != null ? e.getMessage() : "");
        }
    }

    // ── Read path ────────────────────────────────────────────────────

    @Override
    public List<Long> getPresent(Long groupId) {
        try {
            RedisCommands<byte[], byte[]> cmd = redisConnection.sync();
            byte[] key    = key(groupId);
            double now    = Instant.now().getEpochSecond();
            double cutoff = now - HEARTBEAT_TTL_SECONDS;

            // Evict members whose last heartbeat is older than the TTL window.
            cmd.zremrangebyscore(key, Range.create(0.0, cutoff));

            // Return everyone still within the window.
            List<byte[]> active = cmd.zrangebyscore(key,
                    Range.create(cutoff, Double.MAX_VALUE));

            return active.stream()
                    .map(b -> Long.parseLong(new String(b, StandardCharsets.UTF_8)))
                    .toList();
        } catch (Exception e) {
            logger.warn("Presence read failed for group {}: {}",
                    groupId, e.getMessage() != null ? e.getMessage() : "");
            return Collections.emptyList();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static byte[] key(Long groupId) {
        return (KEY_PREFIX + groupId).getBytes(StandardCharsets.UTF_8);
    }
}
