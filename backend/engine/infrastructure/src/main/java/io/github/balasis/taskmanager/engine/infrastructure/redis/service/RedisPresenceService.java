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

// Redis-backed presence tracker. Uses a sorted set per group where each member's
// score is the epoch-second of their last heartbeat. Stale entries are evicted
// on read via ZREMRANGEBYSCORE.
public class RedisPresenceService extends BaseComponent implements PresenceService {

    // Users not seen within this window are treated as offline
    private static final int HEARTBEAT_TTL_SECONDS = 90;

    // Safety TTL on the entire key — auto-deletes when a group goes fully idle
    private static final int KEY_TTL_SECONDS = 300;

    private final String keyPrefix;

    private final StatefulRedisConnection<byte[], byte[]> redisConnection;

    public RedisPresenceService(StatefulRedisConnection<byte[], byte[]> redisConnection,
                                String redisKeyPrefix) {
        this.redisConnection = redisConnection;
        this.keyPrefix = redisKeyPrefix + "presence:";
    }

    // --- write ---

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
            // best-effort, never fail the caller's request
            logger.warn("Presence heartbeat failed for group {}: {}",
                    groupId, e.getMessage() != null ? e.getMessage() : "");
        }
    }

    // --- read ---

    @Override
    public List<Long> getPresent(Long groupId) {
        try {
            RedisCommands<byte[], byte[]> cmd = redisConnection.sync();
            byte[] key    = key(groupId);
            double now    = Instant.now().getEpochSecond();
            double cutoff = now - HEARTBEAT_TTL_SECONDS;

            // evict members older than the TTL
            cmd.zremrangebyscore(key, Range.create(0.0, cutoff));

            // return everyone still within the window
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


    private byte[] key(Long groupId) {
        return (keyPrefix + groupId).getBytes(StandardCharsets.UTF_8);
    }
}
