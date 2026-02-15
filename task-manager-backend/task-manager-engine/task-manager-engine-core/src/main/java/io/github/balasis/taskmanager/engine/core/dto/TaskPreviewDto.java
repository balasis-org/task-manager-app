package io.github.balasis.taskmanager.engine.core.dto;

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
public class TaskPreviewDto {
    private Long id;
    private String title;
    private TaskState taskState;
    private Instant createdAt;
    private Instant dueDate;
    private Long commentCount;
    private Boolean accessible;
    private Boolean newCommentsToBeRead;
}
