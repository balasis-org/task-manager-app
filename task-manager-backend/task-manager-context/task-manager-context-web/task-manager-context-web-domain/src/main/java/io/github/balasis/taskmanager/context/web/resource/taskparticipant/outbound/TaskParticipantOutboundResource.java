package io.github.balasis.taskmanager.context.web.resource.taskparticipant.outbound;


import io.github.balasis.taskmanager.context.base.enumeration.TaskParticipantRole;
import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserOutboundResource;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TaskParticipantOutboundResource extends BaseOutboundResource {
  private UserOutboundResource user;
  private TaskParticipantRole taskParticipantRole;

}
