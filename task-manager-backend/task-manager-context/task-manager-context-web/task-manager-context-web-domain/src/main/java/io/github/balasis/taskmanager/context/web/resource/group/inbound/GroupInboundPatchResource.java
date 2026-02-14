package io.github.balasis.taskmanager.context.web.resource.group.inbound;

import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GroupInboundPatchResource extends BaseInboundResource {
    private String name;
    private String description;
    private String Announcement;
}
