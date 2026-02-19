package io.github.balasis.taskmanager.engine.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonPropertyOrder({"i", "t", "ts", "ca", "dd", "cc", "a", "nc", "cn", "p"})
public class TaskPreviewDto {
    @JsonProperty("i")  private Long id;
    @JsonProperty("t")  private String title;
    @JsonProperty("ts") private TaskState taskState;
    @JsonProperty("ca") private Instant createdAt;
    @JsonProperty("dd") private Instant dueDate;
    @JsonProperty("cc") private Long commentCount;
    @JsonProperty("a")  private Boolean accessible;
    @JsonProperty("nc") private Boolean newCommentsToBeRead;
    @JsonProperty("cn") private String creatorName;
    @JsonProperty("p")  private Integer priority;
}
