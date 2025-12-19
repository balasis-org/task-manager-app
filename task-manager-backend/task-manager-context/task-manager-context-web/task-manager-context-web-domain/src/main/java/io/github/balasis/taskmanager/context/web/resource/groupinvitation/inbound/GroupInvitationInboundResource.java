package io.github.balasis.taskmanager.context.web.resource.groupinvitation.inbound;

import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GroupInvitationInboundResource extends BaseInboundResource {
    @NotBlank
    private Long userId;
}
