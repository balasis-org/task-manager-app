package io.github.balasis.taskmanager.context.web.resource.admin.outbound;

import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AdminTaskParticipantResource extends BaseOutboundResource {
    private Long userId;
    private String userName;
    private String role;
}
