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
 * Two bandwidth limits are enforced <b>atomically</b> in a single Redis
 * round-trip (Bucket4j multi-bandwidth bucket):
 * <ol>
 *   <li><b>Short window</b> — {@link #MAX_PER_MINUTE} tokens per minute.
 *       Catches instant bursts.</li>
 *   <li><b>Sustained window</b> — {@link #MAX_PER_15MIN} tokens per 15 minutes.
 *       Set to 70 % of what a user could consume if they maxed out every single
 *       minute for 15 min (40 × 15 × 0.70 = 420).  Catches sustained abuse
 *       that stays just under the per-minute ceiling.</li>
 * </ol>
 * Both counters are incremented together; if <i>either</i> is exhausted the
 * request is rejected.  15 min was chosen over 30 min to limit Redis memory
 * occupancy for short-lived sessions.
 */
public class RedisRateLimitService extends BaseComponent implements RateLimitService {

    // ──────────────────── tunable limits ───────────────────
    /** Burst ceiling — max requests per minute per IP. */
    private static final int MAX_PER_MINUTE = 40;

    /**
     * Sustained ceiling — max requests per 15 min per IP.
     * 70 % of theoretical maximum (40 × 15 = 600 → 420).
     */
    private static final int MAX_PER_15MIN = 420;
    // ───────────────────────────────────────────────────────

    private final ProxyManager<byte[]> proxyManager;

    private final BucketConfiguration bucketConfiguration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(MAX_PER_MINUTE, Duration.ofMinutes(1)))
            .addLimit(Bandwidth.simple(MAX_PER_15MIN, Duration.ofMinutes(15)))
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
