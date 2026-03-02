package io.github.balasis.taskmanager.engine.infrastructure.redis.service;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.ratelimit.RepeatDownloadBlockedException;
import io.github.balasis.taskmanager.engine.infrastructure.redis.DownloadGuardService;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.nio.charset.StandardCharsets;

// Redis-backed per-file daily download guard. Each user+file pair gets
// an INCR key with a 24-hour TTL. Once the counter exceeds MAX_PER_DAY
// a RepeatDownloadBlockedException is thrown. Best-effort — if Redis is
// unreachable the download is allowed (fail-open, same as PresenceService).
public class RedisDownloadGuardService extends BaseComponent implements DownloadGuardService {

    private static final int MAX_PER_DAY = 5;
    private static final long TTL_SECONDS = 86_400; // 24 hours

    private final StatefulRedisConnection<byte[], byte[]> redisConnection;
    private final String keyPrefix;

    public RedisDownloadGuardService(StatefulRedisConnection<byte[], byte[]> redisConnection,
                                     String redisKeyPrefix) {
        this.redisConnection = redisConnection;
        this.keyPrefix = redisKeyPrefix + "dl:";
    }

    @Override
    public void checkRepeatDownload(long userId, long fileId) {
        try {
            RedisCommands<byte[], byte[]> cmd = redisConnection.sync();
            byte[] key = (keyPrefix + userId + ":" + fileId).getBytes(StandardCharsets.UTF_8);

            Long count = cmd.incr(key);
            if (count == 1L) {
                cmd.expire(key, TTL_SECONDS);
            }
            if (count > MAX_PER_DAY) {
                throw new RepeatDownloadBlockedException(
                        "This file has been downloaded recently. "
                        + "The group's repeat download guard is active "
                        + "\u2014 please try again later.");
            }
        } catch (RepeatDownloadBlockedException e) {
            throw e;
        } catch (Exception e) {
            // best-effort — never block a download because Redis is unavailable
            logger.warn("Download guard check failed for user {} file {}: {}",
                    userId, fileId, e.getMessage() != null ? e.getMessage() : "");
        }
    }
}
