package io.github.balasis.taskmanager.context.web.resource.group.outbound;

import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserOutboundResource;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GroupOutboundResource extends BaseOutboundResource {
    private String name;
    private String description;
    private UserOutboundResource owner;
}
