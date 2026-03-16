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
