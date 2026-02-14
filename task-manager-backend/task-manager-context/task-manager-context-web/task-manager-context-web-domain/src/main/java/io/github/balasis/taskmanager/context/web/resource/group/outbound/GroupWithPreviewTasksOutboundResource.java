package io.github.balasis.taskmanager.context.web.resource.group.outbound;


import io.github.balasis.taskmanager.context.web.resource.task.outbound.TaskPreviewOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserOutboundResource;
import lombok.*;

import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GroupWithPreviewTasksOutboundResource {
    private String name;
    private String description;
    private String defaultImgUrl;
    private String imgUrl;
    private UserOutboundResource owner;
    private String Announcement;
    private Instant createdAt;
    private Set<TaskPreviewOutboundResource> taskPreviewOutboundResourceSet;
}
