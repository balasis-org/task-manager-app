package io.github.balasis.taskmanager.context.web.resource.taskcomment.outbound;


import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserOutboundResource;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskCommentOutboundResource extends BaseOutboundResource {
    private UserOutboundResource creator;
    private String comment;
    private Instant createdAt;
}
