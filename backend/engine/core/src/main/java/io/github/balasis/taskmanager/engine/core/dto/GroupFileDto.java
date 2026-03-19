package io.github.balasis.taskmanager.engine.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

// flat file row for the group files view. merges data from both TaskFile and
// TaskAssigneeFile into one DTO with a "fileType" discriminator. short JSON
// keys like all the other DTOs in this layer.
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonPropertyOrder({"i", "ft", "n", "fs", "ubn", "ubi", "ca", "ti", "tt", "ts", "rv"})
public class GroupFileDto {
    @JsonProperty("i")   private Long id;
    @JsonProperty("ft")  private String fileType;       // "CREATOR" or "ASSIGNEE"
    @JsonProperty("n")   private String name;
    @JsonProperty("fs")  private Long fileSize;
    @JsonProperty("ubn") private String uploadedByName;
    @JsonProperty("ubi") private Long uploadedById;
    @JsonProperty("ca")  private Instant createdAt;
    @JsonProperty("ti")  private Long taskId;
    @JsonProperty("tt")  private String taskTitle;
    @JsonProperty("ts")  private String taskState;
    @JsonProperty("rv")  private List<FileReviewInfoDto> reviews;
}
