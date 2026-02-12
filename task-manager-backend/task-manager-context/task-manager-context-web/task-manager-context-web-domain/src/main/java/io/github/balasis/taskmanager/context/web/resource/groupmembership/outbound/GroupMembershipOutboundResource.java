package io.github.balasis.taskmanager.context.web.resource.groupmembership.outbound;

import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GroupMembershipOutboundResource extends BaseOutboundResource {
    private UserOutboundResource user;
    private Role role;
}
