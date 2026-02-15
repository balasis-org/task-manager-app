package io.github.balasis.taskmanager.context.web.resource.groupinvitation.outbound;

import io.github.balasis.taskmanager.context.base.enumeration.InvitationStatus;
import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserOutboundResource;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GroupInvitationOutboundResource extends BaseOutboundResource {
    private String groupName;
    private UserOutboundResource user;
    private UserOutboundResource invitedBy;
    private InvitationStatus invitationStatus;
    private Role userToBeInvitedRole;

    private String comment;

}
