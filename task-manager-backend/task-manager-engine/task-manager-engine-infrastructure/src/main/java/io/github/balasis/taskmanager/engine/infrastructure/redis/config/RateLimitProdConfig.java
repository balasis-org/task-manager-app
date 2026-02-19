package io.github.balasis.taskmanager.engine.infrastructure.redis.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.github.balasis.taskmanager.engine.infrastructure.redis.RateLimitService;
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

/**
 * Wires the Redis-backed rate limiter for <b>prod</b> profiles.
 * <p>
 * Identical to the dev config for now — when prod needs a different
 * Redis instance or different credentials just update this class.
 * <p>
 * If the Redis connection fails the bean falls back to a no-op
 * implementation so the application still starts.
 */
@Configuration
@Profile({"prod-h2", "prod-azuresql"})
@RequiredArgsConstructor
public class RateLimitProdConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimitProdConfig.class);

    private final SecretClientProvider secretClientProvider;

    @Bean
    public RateLimitService rateLimitService() {
        try {
            String endpoint  = secretClientProvider.getSecret("RedisEndpoint");
            String accessKey = secretClientProvider.getSecret("RedisAccessKey");

            if (endpoint == null || endpoint.isBlank()
                    || accessKey == null || accessKey.isBlank()) {
                log.warn("Redis endpoint or access key not configured — rate limiting disabled");
                return key -> {};
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
            log.warn("Failed to connect to Redis — rate limiting disabled: {}", e.getMessage());
            return key -> {};
        }
    }
}
