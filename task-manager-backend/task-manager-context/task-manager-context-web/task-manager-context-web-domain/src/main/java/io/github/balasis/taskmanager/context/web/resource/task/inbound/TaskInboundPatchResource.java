package io.github.balasis.taskmanager.context.web.resource.task.inbound;

import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TaskInboundPatchResource extends BaseInboundResource {
    private String title;
    private String description;
    private TaskState taskState;
}
