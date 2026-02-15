package io.github.balasis.taskmanager.context.web.resource.groupevent.outbound;

import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GroupEventOutboundResource extends BaseOutboundResource {
    private String description;
    private Instant createdAt;
}
