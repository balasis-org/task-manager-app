package io.github.balasis.taskmanager.engine.infrastructure.redis;

import java.util.List;

// Tracks which users are currently viewing a group.
public interface PresenceService {

    // records a heartbeat for the user in the group (best-effort, wont fail the caller)
    void heartbeat(Long groupId, Long userId);

    // returns user IDs of everyone currently present (heartbeat within the TTL window)
    List<Long> getPresent(Long groupId);
}
