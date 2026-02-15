package io.github.balasis.taskmanager.context.web.resource.groupinvitation.inbound;

import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GroupInvitationInboundResource extends BaseInboundResource {
    @NotNull
    @Positive
    private Long userId;

    private Role userToBeInvitedRole;

    private String comment;
}
