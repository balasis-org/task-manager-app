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
@JsonPropertyOrder({"i", "n", "d", "diu", "iu", "oi", "on", "an", "ca", "tp"})
public class GroupWithPreviewDto {
    @JsonProperty("i")   private Long id;
    @JsonProperty("n")   private String name;
    @JsonProperty("d")   private String description;
    @JsonProperty("diu") private String defaultImgUrl;
    @JsonProperty("iu")  private String imgUrl;
    @JsonProperty("oi")  private Long ownerId;
    @JsonProperty("on")  private String ownerName;
    @JsonProperty("an")  private String announcement;
    @JsonProperty("ca")  private Instant createdAt;
    @JsonProperty("tp")  private Set<TaskPreviewDto> taskPreviews;
}
