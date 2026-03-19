package io.github.balasis.taskmanager.engine.infrastructure.redis.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.github.balasis.taskmanager.engine.infrastructure.redis.DownloadGuardService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.EmailDrainLockService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.ImageChangeLimiterService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.CommentAnalysisLockService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.ImageModerationLockService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.PresenceService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.RateLimitService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.service.RedisDownloadGuardService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.service.RedisEmailDrainLockService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.service.RedisImageChangeLimiterService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.service.RedisCommentAnalysisLockService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.service.RedisImageModerationLockService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.service.RedisPresenceService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.service.RedisRateLimitService;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

// mega config: creates the shared Redis connection (Azure Cache for Redis, TLS, Key Vault credentials)
// plus every Redis-backed bean: rate limiter, presence tracker, download guard, image change limiter,
// and all three distributed locks (email, moderation, analysis).
//
// uses Lettuce (not Jedis) as the Redis driver because Bucket4j's Redis integration
// (bucket4j-lettuce) requires it. Lettuce is non-blocking under the hood but we use
// the sync() command API for simplicity.
//
// ByteArrayCodec: Bucket4j stores its own binary bucket state in Redis, so we skip
// the default UTF-8 string codec and use raw byte arrays to avoid encoding overhead.
//
// connection is shared across all beans — Lettuce connections are thread-safe
// (they multiplex commands over a single TCP connection).
@Configuration
@Profile({"prod-h2", "prod-azuresql", "prod-arena-stress", "prod-arena-security"})
@RequiredArgsConstructor
public class RateLimitProdConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimitProdConfig.class);

    private final SecretClientProvider secretClientProvider;

    @Bean
    public StatefulRedisConnection<byte[], byte[]> redisConnection() {
        String endpoint  = secretClientProvider.getSecret("TASKMANAGER-REDIS-ENDPOINT");
        String accessKey = secretClientProvider.getSecret("TASKMANAGER-REDIS-ACCESS-KEY");

        if (endpoint == null || endpoint.isBlank()
                || accessKey == null || accessKey.isBlank()) {
            throw new IllegalStateException(
                "Redis endpoint and access key must be configured for rate limiting");
        }

        String[] parts = endpoint.split(":", 2);
        String host = parts[0];
        int port = (parts.length > 1) ? Integer.parseInt(parts[1]) : 10000;

        // Azure Cache for Redis uses TLS on port 10000 (non-SSL: 6380, but we always use SSL).
        // "default" is the Redis 6+ ACL username for password-only auth.
        RedisURI uri = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withSsl(true)
                .withAuthentication("default", accessKey)
                .build();

        RedisClient client = RedisClient.create(uri);
        StatefulRedisConnection<byte[], byte[]> connection =
                client.connect(new ByteArrayCodec());

        log.info("Redis connected to {}:{}", host, port);
        return connection;
    }

    @Bean
    public RateLimitService rateLimitService(StatefulRedisConnection<byte[], byte[]> redisConnection) {
        try {
            // LettuceBasedProxyManager: the Bucket4j adapter that stores token-bucket state
            // as binary blobs in Redis. each user's bucket is a single Redis key.
            //
            // ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax:
            //   sets a Redis TTL equal to the time needed to fully refill the bucket.
            //   (here: 16 min > our 15-min window, so buckets auto-expire when idle
            //   and dont litter Redis with stale keys)
            LettuceBasedProxyManager<byte[]> proxyManager = LettuceBasedProxyManager
                    .builderFor(redisConnection)
                    .withExpirationStrategy(
                            ExpirationAfterWriteStrategy
                                    .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(16))
                    )
                    .build();

            log.info("Redis rate limiter initialized");
            return new RedisRateLimitService(proxyManager);

        } catch (Exception e) {
            throw new IllegalStateException(
                "Redis is required for rate limiting but failed to connect: " + e.getMessage(), e);
        }
    }

    @Bean
    public PresenceService presenceService(StatefulRedisConnection<byte[], byte[]> redisConnection) {
        return new RedisPresenceService(redisConnection, "");
    }

    @Bean
    public DownloadGuardService downloadGuardService(StatefulRedisConnection<byte[], byte[]> redisConnection) {
        return new RedisDownloadGuardService(redisConnection, "");
    }

    @Bean
    public ImageChangeLimiterService imageChangeLimiterService(StatefulRedisConnection<byte[], byte[]> redisConnection) {
        return new RedisImageChangeLimiterService(redisConnection, "");
    }

    @Bean
    public EmailDrainLockService emailDrainLockService(StatefulRedisConnection<byte[], byte[]> redisConnection) {
        return new RedisEmailDrainLockService(redisConnection);
    }

    @Bean
    public ImageModerationLockService imageModerationLockService(StatefulRedisConnection<byte[], byte[]> redisConnection) {
        return new RedisImageModerationLockService(redisConnection);
    }

    @Bean
    public CommentAnalysisLockService commentAnalysisLockService(StatefulRedisConnection<byte[], byte[]> redisConnection) {
        return new RedisCommentAnalysisLockService(redisConnection);
    }
}
