package io.github.balasis.taskmanager.context.web.resource.task.inbound;

import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskInboundResource extends BaseInboundResource {

    @NotBlank(message = "title is mandatory")
    @Size(max = 150, message = "title must be at most 150 characters")
    private String title;

    @NotBlank(message = "description is mandatory")
    @Size(max = 1500, message = "description must be at most 1500 characters")
    private String description;

    @NotNull(message = "taskState is mandatory")
    private TaskState taskState;
    private Set<Long> assignedIds;
    private Set<Long> reviewerIds;
    private Instant dueDate;
    @Min(value = 0, message = "priority must be between 0 and 10")
    @Max(value = 10, message = "priority must be between 0 and 10")
    private Integer priority;
}