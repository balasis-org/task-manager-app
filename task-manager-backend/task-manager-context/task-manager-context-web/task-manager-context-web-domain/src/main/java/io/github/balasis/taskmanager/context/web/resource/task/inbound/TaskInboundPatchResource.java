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
}
