package io.github.balasis.taskmanager.context.web.resource.taskparticipant.inbound;

import io.github.balasis.taskmanager.context.base.enumeration.TaskParticipantRole;
import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TaskParticipantInboundResource extends BaseInboundResource {
    @NotNull
    private Long userId;

    @NotNull
    private TaskParticipantRole taskParticipantRole;
}
