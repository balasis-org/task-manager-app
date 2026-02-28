package io.github.balasis.taskmanager.engine.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonPropertyOrder({"sn", "c", "mc", "n", "d", "an", "diu", "iu", "aen", "lged", "ct", "dti"})
public class GroupRefreshDto {

    @JsonProperty("sn")   private Instant serverNow;

    @JsonProperty("c")    private boolean changed;

    @JsonProperty("mc")   private boolean membersChanged;

    @JsonProperty("n")    private String name;
    @JsonProperty("d")    private String description;
    @JsonProperty("an")   private String announcement;
    @JsonProperty("diu")  private String defaultImgUrl;
    @JsonProperty("iu")   private String imgUrl;
    @JsonProperty("aen")  private Boolean allowEmailNotification;
    @JsonProperty("lged") private Instant lastGroupEventDate;

    @JsonProperty("ct")   private Set<TaskPreviewDto> changedTasks;

    @JsonProperty("dti")  private Set<Long> deletedTaskIds;
}
