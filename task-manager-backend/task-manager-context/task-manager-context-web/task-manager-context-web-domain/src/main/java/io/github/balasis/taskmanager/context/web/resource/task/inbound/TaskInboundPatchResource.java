package io.github.balasis.taskmanager.context.web.resource.task.inbound;

import io.github.balasis.taskmanager.context.base.enumeration.ReviewersDecision;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskInboundPatchResource extends BaseInboundResource {
    private String title;
    private String description;
    private TaskState taskState;
    private ReviewersDecision reviewersDecision;
    private String reviewComment;
    private Instant dueDate;
    @jakarta.validation.constraints.Min(value = 0, message = "priority must be between 0 and 10")
    @jakarta.validation.constraints.Max(value = 10, message = "priority must be between 0 and 10")
    private Integer priority;
}
