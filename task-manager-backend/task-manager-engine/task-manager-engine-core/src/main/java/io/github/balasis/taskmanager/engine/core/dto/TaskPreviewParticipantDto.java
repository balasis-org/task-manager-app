package io.github.balasis.taskmanager.engine.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonPropertyOrder({"ui", "un", "r"})
public class TaskPreviewParticipantDto {
    @JsonProperty("ui") private Long userId;
    @JsonProperty("un") private String userName;
    @JsonProperty("r")  private String role;
}
