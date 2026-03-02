package io.github.balasis.taskmanager.engine.infrastructure.redis.service;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.business.BusinessRuleException;
import io.github.balasis.taskmanager.engine.infrastructure.redis.ImageChangeLimiterService;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.nio.charset.StandardCharsets;

/**
 * Redis INCR + EXPIRE implementation. Allows a burst of up to 5 image
 * changes then blocks until the 5-minute window expires. Fail-open:
 * any Redis error is logged but does not block the upload — the global
 * rate limiter (fail-closed) is the last line of defence.
 */
public class RedisImageChangeLimiterService extends BaseComponent implements ImageChangeLimiterService {

    private static final int MAX_PER_WINDOW = 5;
    private static final long WINDOW_SECONDS = 300; // 5 minutes

    private final StatefulRedisConnection<byte[], byte[]> redisConnection;
    private final String keyPrefix;

    public RedisImageChangeLimiterService(StatefulRedisConnection<byte[], byte[]> redisConnection,
                                          String redisKeyPrefix) {
        this.redisConnection = redisConnection;
        this.keyPrefix = redisKeyPrefix + "img:";
    }

    @Override
    public void checkBurstLimit(long userId) {
        try {
            RedisCommands<byte[], byte[]> cmd = redisConnection.sync();
            byte[] key = (keyPrefix + userId).getBytes(StandardCharsets.UTF_8);

            Long count = cmd.incr(key);
            if (count == 1L) {
                cmd.expire(key, WINDOW_SECONDS);
            }
            if (count > MAX_PER_WINDOW) {
                throw new BusinessRuleException(
                        "Too many image changes \u2014 please wait a moment and try again.");
            }
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Image burst limiter check failed for user {}: {}",
                    userId, e.getMessage() != null ? e.getMessage() : "");
            // fail-open: global rate limiter is already fail-closed
        }
    }
}
