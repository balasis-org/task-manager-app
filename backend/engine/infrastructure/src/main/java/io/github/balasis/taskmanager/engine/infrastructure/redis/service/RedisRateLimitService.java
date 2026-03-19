package io.github.balasis.taskmanager.engine.infrastructure.redis.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.critical.CriticalInfrastructureException;
import io.github.balasis.taskmanager.context.base.exception.ratelimit.RateLimitExceededException;
import io.github.balasis.taskmanager.engine.infrastructure.redis.RateLimitService;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

// Bucket4j-based per-key rate limiter backed by Redis (Lettuce driver).
// Bucket4j implements the *token bucket* algorithm: each bucket starts full of tokens,
// every request consumes one token, and tokens refill at a fixed rate. when the bucket
// is empty the request is rejected. storing buckets in Redis means all app instances
// share the same counters — no per-instance drift.
//
// dual-bandwidth config: two limits enforced simultaneously.
//   - 40 requests / 1 minute  (burst protection)
//   - 420 requests / 15 minutes (sustained abuse protection)
// both must have tokens available or the request is rejected.
//
// fail-closed: if Redis is unreachable, the request is rejected (not allowed through).
// this is the last line of defence so it must not silently pass — all other guards
// (download guard, image limiter) are fail-open because this one backstops them.
public class RedisRateLimitService extends BaseComponent implements RateLimitService {

    private static final int MAX_PER_MINUTE = 40;

    private static final int MAX_PER_15MIN = 420;

    // ProxyManager is the Bucket4j abstraction over Redis storage.
    // it creates/retrieves BucketProxy instances (one per key) that map to
    // Redis data structures managed entirely by the Bucket4j library.
    private final ProxyManager<byte[]> proxyManager;

    // BucketConfiguration holds the dual-bandwidth limits.
    // Bandwidth.simple(n, duration) = "n tokens refilled over duration".
    // when both limits are present, BOTH must have available tokens for a request to succeed.
    private final BucketConfiguration bucketConfiguration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(MAX_PER_MINUTE, Duration.ofMinutes(1)))    // 40 tokens refill every minute
            .addLimit(Bandwidth.simple(MAX_PER_15MIN, Duration.ofMinutes(15)))   // 420 tokens refill every 15 min
            .build();

    public RedisRateLimitService(ProxyManager<byte[]> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    public void checkRateLimit(String key) {
        try {
            // build() fetches or creates the bucket state in Redis for this key.
            // the lambda provides the initial config if the bucket doesn't exist yet.
            BucketProxy bucket = proxyManager.builder()
                    .build(key.getBytes(StandardCharsets.UTF_8), () -> bucketConfiguration);

            // tryConsumeAndReturnRemaining atomically decrements the token count.
            // ConsumptionProbe tells us if the token was consumed and how long until
            // the next refill if it wasnt (for the Retry-After header).
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
            throw e;
        } catch (Exception e) {

            logger.error("Rate limiter unavailable, rejecting request: {}",
                    e.getMessage() != null ? e.getMessage() : "");
            throw new CriticalInfrastructureException(
                "Redis rate limiter unavailable, rejecting request");
        }
    }
}
