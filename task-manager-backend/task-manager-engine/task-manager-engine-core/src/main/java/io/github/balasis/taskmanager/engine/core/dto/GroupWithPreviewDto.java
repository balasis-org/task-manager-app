package io.github.balasis.taskmanager.engine.core.dto;

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
public class GroupWithPreviewDto {
    private Long id;
    private String name;
    private String description;
    private String defaultImgUrl;
    private String imgUrl;
    private Long ownerId;
    private String ownerName;
    private String announcement;
    private Instant createdAt;
    private Set<TaskPreviewDto> taskPreviews;
}
