package io.github.balasis.taskmanager.engine.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskPreviewParticipantDto {
    private Long userId;
    private String userName;
    private String role;
}
