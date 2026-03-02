package io.github.balasis.taskmanager.context.web.throttle;

import io.github.balasis.taskmanager.context.base.exception.business.LimitExceededException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Caps how many downloads a single user can run at the same time.
 *
 * Without this a single user (or attacker) could spam-click download on
 * every file and monopolise the entire async-download thread pool,
 * starving everyone else out. We keep per-user counts in a
 * ConcurrentHashMap so different users never contend with each other and
 * same-user contention is just an atomic CAS — negligible overhead.
 *
 * Edge case we accept: if acquire() succeeds on the servlet thread but
 * Spring then rejects the StreamingResponseBody (queue-full → 503), the
 * counter stays +1 because the lambda's finally block never runs. With
 * queue=100 that needs 115+ simultaneous downloads to trigger, and even
 * then the user just loses one slot until the next restart. Not worth a
 * background-cleanup for something that basically can't happen.
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
