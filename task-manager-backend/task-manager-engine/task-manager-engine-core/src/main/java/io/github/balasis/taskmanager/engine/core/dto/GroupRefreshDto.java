package io.github.balasis.taskmanager.engine.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

/**
 * Returned by the refresh endpoint.
 * Contains only what changed since the client's lastSeen timestamp.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupRefreshDto {
    /** Server timestamp to store as the new lastSeen. */
    private Instant serverNow;

    /** True when anything in the group changed since lastSeen. If false, everything below is empty/null. */
    private boolean changed;

    /** True when the membership list changed since lastSeen (member added/removed/role changed). */
    private boolean membersChanged;

    // ── Group-level fields (only populated when the group itself changed) ──
    private String name;
    private String description;
    private String announcement;
    private String defaultImgUrl;
    private String imgUrl;
    private Boolean allowEmailNotification;
    private Instant lastGroupEventDate;

    // ── Tasks that were created or updated since lastSeen ──
    private Set<TaskPreviewDto> changedTasks;

    // ── IDs of tasks that were deleted since lastSeen ──
    private Set<Long> deletedTaskIds;
}
