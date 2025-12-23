package io.github.balasis.taskmanager.context.web.resource.user.inbound;

import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserInboundResource extends BaseInboundResource {
    private String name;
}
