package io.github.balasis.taskmanager.engine.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

/**
 * returned by the refresh endpoint.
 * Contains only what changed since the client  lastSeen timestamp.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonPropertyOrder({"sn", "c", "mc", "n", "d", "an", "diu", "iu", "aen", "lged", "ct", "dti"})
public class GroupRefreshDto {
    /** Server timestamp to store as the new lastSeen. */
    @JsonProperty("sn")   private Instant serverNow;

    /** True when anything in the group changed since lastSeen. If false, everything below is empty/null */
    @JsonProperty("c")    private boolean changed;

    /** True when the membership list changed since lastSeen (member added/removed/role changed) */
    @JsonProperty("mc")   private boolean membersChanged;

    // group-level fields (only populated when the group itself changed) ──
    @JsonProperty("n")    private String name;
    @JsonProperty("d")    private String description;
    @JsonProperty("an")   private String announcement;
    @JsonProperty("diu")  private String defaultImgUrl;
    @JsonProperty("iu")   private String imgUrl;
    @JsonProperty("aen")  private Boolean allowEmailNotification;
    @JsonProperty("lged") private Instant lastGroupEventDate;

    // tsks that were created or updated since lastSeen ──
    @JsonProperty("ct")   private Set<TaskPreviewDto> changedTasks;

    // Ids of tasks that were deleted since lastSeen ──
    @JsonProperty("dti")  private Set<Long> deletedTaskIds;
}
