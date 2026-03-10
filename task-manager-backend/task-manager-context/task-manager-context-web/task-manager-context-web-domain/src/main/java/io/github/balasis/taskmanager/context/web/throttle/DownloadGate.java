package io.github.balasis.taskmanager.context.web.throttle;

import io.github.balasis.taskmanager.context.base.exception.business.LimitExceededException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-user concurrent-download limiter backed by ConcurrentHashMap + AtomicInteger.
 * Prevents one user from hogging the whole async thread pool.
 */
@Component
public class DownloadGate {

    private static final int MAX_CONCURRENT_PER_USER = 3;

    private final ConcurrentHashMap<Long, AtomicInteger> active = new ConcurrentHashMap<>();

    /**
     * Reserves a download slot for this user. Throws immediately if they
     * already have {@value MAX_CONCURRENT_PER_USER} downloads in flight.
     */
    public void acquire(Long userId) {
        AtomicInteger count = active.computeIfAbsent(userId, k -> new AtomicInteger(0));
        int now = count.incrementAndGet();
        if (now > MAX_CONCURRENT_PER_USER) {
            count.decrementAndGet(); // undo — they're over the limit
            throw new LimitExceededException(
                    "Too many concurrent downloads — please wait for one to finish before starting another");
        }
    }

    /**
     * Releases a download slot. Called in the finally block of the
     * StreamingResponseBody so it fires after the transfer completes
     * (or errors out) on the async thread.
     */
    public void release(Long userId) {
        // atomically decrement; remove the entry entirely when it hits 0
        // so the map doesn't grow unbounded over time
        active.computeIfPresent(userId, (key, count) ->
                count.decrementAndGet() <= 0 ? null : count);
    }
}
