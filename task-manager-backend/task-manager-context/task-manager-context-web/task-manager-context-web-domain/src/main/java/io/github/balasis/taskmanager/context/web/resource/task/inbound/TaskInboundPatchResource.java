package io.github.balasis.taskmanager.context.web.resource.task.inbound;

import io.github.balasis.taskmanager.context.base.enumeration.ReviewersDecision;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskInboundPatchResource extends BaseInboundResource {
    @Size(max = 150, message = "title must be at most 150 characters")
    private String title;

    @Size(max = 1500, message = "description must be at most 1500 characters")
    private String description;
    private TaskState taskState;
    private ReviewersDecision reviewersDecision;

    @Size(max = 400, message = "review comment must be at most 400 characters")
    private String reviewComment;
    private Instant dueDate;
    @jakarta.validation.constraints.Min(value = 0, message = "priority must be between 0 and 10")
    @jakarta.validation.constraints.Max(value = 10, message = "priority must be between 0 and 10")
    private Integer priority;
}
