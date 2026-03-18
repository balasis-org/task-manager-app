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

// Redis-backed presence tracker using a Sorted Set (ZSET) per group.
//
// Redis sorted sets: each member has a score (a double). members are unique,
// scores can repeat. we use the userId as the member and their last-heartbeat
// epoch-second as the score. this lets us:
//   ZADD key score member     — upsert the heartbeat timestamp
//   ZREMRANGEBYSCORE key 0 X  — evict everyone older than X (stale users)
//   ZRANGEBYSCORE key X +inf  — get everyone still within the window
//
// the whole key gets a safety TTL (5 min) so it auto-deletes if everyone
// leaves and no more heartbeats come in — prevents key leaks.
//
// this is NOT pub/sub or SSE — it's pure query-on-demand. the frontend polls
// the /groups/{id}/presence endpoint every 30-60s via the smart poll hook.
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

            // ZADD: if the member exists, just updates its score (last-seen timestamp).
            // if it doesn't exist, adds it. O(log N) for the sorted set.
            cmd.zadd(key, score, member);
            // EXPIRE resets the safety TTL so the key doesn't die while users are active
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

            // ZREMRANGEBYSCORE: removes all members with score between 0 and cutoff.
            // this evicts users whose last heartbeat is older than HEARTBEAT_TTL_SECONDS.
            cmd.zremrangebyscore(key, Range.create(0.0, cutoff));

            // ZRANGEBYSCORE: returns all members with score >= cutoff (still alive).
            // Double.MAX_VALUE = "+inf" in Redis terms.
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
