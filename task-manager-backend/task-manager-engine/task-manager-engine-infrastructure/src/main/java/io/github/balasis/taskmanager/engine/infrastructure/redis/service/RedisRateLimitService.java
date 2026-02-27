package io.github.balasis.taskmanager.engine.infrastructure.redis.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.ratelimit.RateLimitExceededException;
import io.github.balasis.taskmanager.context.base.exception.ratelimit.ServiceOverloadedException;
import io.github.balasis.taskmanager.engine.infrastructure.redis.RateLimitService;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class RedisRateLimitService extends BaseComponent implements RateLimitService {

    private static final int MAX_PER_MINUTE = 40;

    private static final int MAX_PER_15MIN = 420;

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
            throw e;
        } catch (Exception e) {

            logger.error("Rate limiter unavailable — rejecting request: {}",
                    e.getMessage() != null ? e.getMessage() : "");
            throw new ServiceOverloadedException(
                "We are sorry but currently we are under heavy traffic. " +
                "We cannot accept your request at this time, please try again later.");
        }
    }
}
