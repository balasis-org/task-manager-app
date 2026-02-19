package io.github.balasis.taskmanager.engine.infrastructure.redis.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.ratelimit.RateLimitExceededException;
import io.github.balasis.taskmanager.engine.infrastructure.redis.RateLimitService;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Distributed token-bucket rate limiter backed by Redis (via Bucket4j + Lettuce).
 * <p>
 * Adjust {@link #MAX_REQUESTS_PER_MINUTE} to raise / lower the global per-IP limit.
 */
public class RedisRateLimitService extends BaseComponent implements RateLimitService {

    // ──────────────────── tunable limit ────────────────────
    /**
     * Maximum requests any single IP may make per minute.
     * <p>
     * Estimation (100-user task-manager, 67 endpoints):
     * <ul>
     *   <li>4 lightweight polling endpoints × ~1 req/min = 4 req/min passive</li>
     *   <li>Active CRUD / navigation bursts ≈ 15-20 req/min peak</li>
     *   <li>Authentication refresh, invite-code refresh, file uploads ≈ occasional</li>
     * </ul>
     * 40 req/min is sufficient for normal use while dampening abuse.
     */
    private static final int MAX_REQUESTS_PER_MINUTE = 40;
    // ───────────────────────────────────────────────────────

    private final ProxyManager<byte[]> proxyManager;

    private final BucketConfiguration bucketConfiguration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1)))
            .build();

    public RedisRateLimitService(ProxyManager<byte[]> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    public void checkRateLimit(String key) {
        try {
            BucketProxy bucket = proxyManager.builder()
                    .build(key.getBytes(StandardCharsets.UTF_8), () -> bucketConfiguration);

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            if (!probe.isConsumed()) {
                long waitSeconds = Math.max(1,
                        TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
                throw new RateLimitExceededException(
                        "Too many requests. Please wait " + waitSeconds + " seconds.",
                        waitSeconds
                );
            }
        } catch (RateLimitExceededException e) {
            throw e; // propagate our own exception
        } catch (Exception e) {
            // Fail-open: if Redis is down we allow the request and log a warning.
            logger.warn("Rate limiter unavailable — allowing request: {}", e.getMessage());
        }
    }
}
