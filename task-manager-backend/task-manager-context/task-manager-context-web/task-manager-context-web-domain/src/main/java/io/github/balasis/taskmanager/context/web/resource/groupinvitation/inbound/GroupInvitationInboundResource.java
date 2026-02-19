package io.github.balasis.taskmanager.context.web.resource.groupinvitation.inbound;

import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GroupInvitationInboundResource extends BaseInboundResource {
    @NotBlank(message = "Invite code is required")
    @Size(max = 8, message = "Invite code must be at most 8 characters")
    private String inviteCode;

    private Role userToBeInvitedRole;

    @Size(max = 400, message = "comment must be at most 400 characters")
    private String comment;
}
