package io.github.balasis.taskmanager.engine.infrastructure.redis.service;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.ratelimit.RepeatDownloadBlockedException;
import io.github.balasis.taskmanager.engine.infrastructure.redis.DownloadGuardService;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

// Redis-backed per-file download guard — one hash per user.
//
// Structure:
//   Key  = dl:{userId}          (one key per active user)
//   _w   = window start epoch   (shared window for the whole hash)
//   {id} = download count       (one field per file touched in this window)
//   TTL  = 9 h safety net
//
// When the 8-hour window expires the entire hash is deleted, wiping
// all file entries in one shot. Within a window each file is capped
// at 3 blob downloads — ETag 304s never reach this guard.
//
// Worst-case hash size within one window is bounded by the user's
// download budget (50 GB/month for TEAM), not by file count: the
// budget will run out long before the hash can grow meaningfully.
//
// Best-effort — if Redis is unreachable the download is allowed.
public class RedisDownloadGuardService extends BaseComponent implements DownloadGuardService {

    private static final int  MAX_PER_WINDOW  = 3;
    private static final long WINDOW_SECONDS  = 28_800;  // 8 hours
    private static final long KEY_TTL_SECONDS = 32_400;  // 9 hours safety

    private static final byte[] WINDOW_FIELD = "_w".getBytes(StandardCharsets.UTF_8);

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
            byte[] key   = (keyPrefix + userId).getBytes(StandardCharsets.UTF_8);
            byte[] field = String.valueOf(fileId).getBytes(StandardCharsets.UTF_8);
            long   now   = Instant.now().getEpochSecond();

            // check if the window is still alive
            byte[] windowRaw = cmd.hget(key, WINDOW_FIELD);
            if (windowRaw != null) {
                long windowStart = Long.parseLong(new String(windowRaw, StandardCharsets.UTF_8));
                if (now - windowStart < WINDOW_SECONDS) {
                    // window is valid — check file count
                    byte[] countRaw = cmd.hget(key, field);
                    int count = countRaw == null ? 0
                            : Integer.parseInt(new String(countRaw, StandardCharsets.UTF_8));

                    if (count >= MAX_PER_WINDOW) {
                        throw new RepeatDownloadBlockedException(
                                "This file has been downloaded " + MAX_PER_WINDOW
                                + " times recently. The group's repeat download guard "
                                + "is active \u2014 please try again later.");
                    }

                    cmd.hset(key, field, toBytes(count + 1));
                    cmd.expire(key, KEY_TTL_SECONDS);
                    return;
                }
                // window expired — nuke everything and start fresh
                cmd.del(key);
            }

            // first download or window just reset
            cmd.hset(key, WINDOW_FIELD, toBytes(now));
            cmd.hset(key, field, toBytes(1));
            cmd.expire(key, KEY_TTL_SECONDS);
        } catch (RepeatDownloadBlockedException e) {
            throw e;
        } catch (Exception e) {
            // best-effort — never block a download because Redis is unavailable
            logger.warn("Download guard check failed for user {} file {}: {}",
                    userId, fileId, e.getMessage() != null ? e.getMessage() : "");
        }
    }

    private static byte[] toBytes(long value) {
        return String.valueOf(value).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] toBytes(int value) {
        return String.valueOf(value).getBytes(StandardCharsets.UTF_8);
    }
}
