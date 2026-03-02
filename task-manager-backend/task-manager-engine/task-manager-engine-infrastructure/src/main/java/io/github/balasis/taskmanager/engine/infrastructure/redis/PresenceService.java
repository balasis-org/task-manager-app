package io.github.balasis.taskmanager.engine.infrastructure.redis;

import java.util.List;

/**
 * Tracks which users are currently viewing a group (online presence).
 *
 * Backed by a Redis sorted set per group.  Each member's score is the
 * epoch-second of their last heartbeat.  Members whose heartbeat is
 * older than the TTL window are considered offline and automatically
 * evicted on the next read.
 */
public interface PresenceService {

    /**
     * Records a heartbeat for the given user in the given group.
     * Called as a side-effect of the existing polling endpoints —
     * no extra HTTP request from the frontend.
     *
     * Best-effort: silently swallows Redis failures so the main
     * request is never affected.
     */
    void heartbeat(Long groupId, Long userId);

    /**
     * Returns the user IDs of everyone currently present in the group
     * (heartbeat within the last {@code HEARTBEAT_TTL_SECONDS} seconds).
     *
     * Evicts stale entries before reading. Returns an empty list on
     * Redis failure.
     */
    List<Long> getPresent(Long groupId);
}
