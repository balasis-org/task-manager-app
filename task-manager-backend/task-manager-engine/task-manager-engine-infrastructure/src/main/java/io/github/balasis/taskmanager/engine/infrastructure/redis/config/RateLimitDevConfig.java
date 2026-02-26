package io.github.balasis.taskmanager.engine.infrastructure.redis.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.github.balasis.taskmanager.engine.infrastructure.redis.RateLimitService;
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

/**
 * Wires the Redis-backed rate limiter for <b>dev</b> profiles using a
 * local Docker Redis container (no SSL, password auth via env vars).
 * <p>
 * Expected env vars:
 * <ul>
 *   <li>{@code REDIS_HOST} — defaults to {@code localhost}</li>
 *   <li>{@code REDIS_PORT} — defaults to {@code 6379}</li>
 *   <li>{@code REDIS_PASSWORD} — optional; when blank, no auth is used</li>
 * </ul>
 * If the Redis connection fails the bean falls back to a no-op
 * implementation so the application still starts.
 */
@Configuration
@Profile({"dev-h2", "dev-mssql", "dev-flyway-mssql"})
@RequiredArgsConstructor
public class RateLimitDevConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimitDevConfig.class);

    @Bean
    public RateLimitService rateLimitService() {
        try {
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

            LettuceBasedProxyManager<byte[]> proxyManager = LettuceBasedProxyManager
                    .builderFor(connection)
                    .withExpirationStrategy(
                            ExpirationAfterWriteStrategy
                                    .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(16))
                    )
                    .build();

            log.info("Redis rate limiter connected to {}:{}", host, port);
            return new RedisRateLimitService(proxyManager);

        } catch (Exception e) {
            throw new IllegalStateException(
                "Redis is required for rate limiting but failed to connect: " + e.getMessage(), e);
        }
    }
}
