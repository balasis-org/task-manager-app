package io.github.balasis.taskmanager.engine.infrastructure.redis.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.github.balasis.taskmanager.engine.infrastructure.redis.DownloadGuardService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.ImageChangeLimiterService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.PresenceService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.RateLimitService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.service.RedisDownloadGuardService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.service.RedisImageChangeLimiterService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.service.RedisPresenceService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.service.RedisRateLimitService;
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
@Profile({"dev-h2", "dev-mssql", "dev-flyway-mssql"})
@RequiredArgsConstructor
public class RateLimitDevConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimitDevConfig.class);

    @Bean
    public StatefulRedisConnection<byte[], byte[]> redisConnection() {
        String host = System.getenv("REDIS_HOST");
        String portStr = System.getenv("REDIS_PORT");
        String password = System.getenv("REDIS_PASSWORD");

        if (host == null || host.isBlank()) host = "localhost";
        int port;
        try {
            port = (portStr != null && !portStr.isBlank()) ? Integer.parseInt(portStr) : 6379;
        } catch (NumberFormatException e) {
            port = 6379;
        }

        RedisURI.Builder uriBuilder = RedisURI.builder()
                .withHost(host)
                .withPort(port);

        if (password != null && !password.isBlank()) {
            uriBuilder.withPassword(password.toCharArray());
        }

        RedisURI uri = uriBuilder.build();

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
}
