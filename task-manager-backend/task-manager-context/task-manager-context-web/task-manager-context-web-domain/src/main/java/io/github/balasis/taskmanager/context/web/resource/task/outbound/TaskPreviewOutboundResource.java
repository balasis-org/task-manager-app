package io.github.balasis.taskmanager.context.web.resource.task.outbound;

import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.taskparticipant.outbound.TaskParticipantOutboundResource;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TaskPreviewOutboundResource extends BaseOutboundResource {
    private String title;
    private String taskState;
    private Set<TaskParticipantOutboundResource> taskParticipants;
}
