package io.github.balasis.taskmanager.context.web.resource.task.inbound;

import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private String title;

    @NotBlank(message = "description is mandatory")
    private String description;

    @NotNull(message = "taskState is mandatory")
    private TaskState taskState;
    private Set<Long> assignedIds;
    private Set<Long> reviewerIds;
    private Instant dueDate;
    @jakarta.validation.constraints.Min(value = 0, message = "priority must be between 0 and 10")
    @jakarta.validation.constraints.Max(value = 10, message = "priority must be between 0 and 10")
    private Integer priority;
}