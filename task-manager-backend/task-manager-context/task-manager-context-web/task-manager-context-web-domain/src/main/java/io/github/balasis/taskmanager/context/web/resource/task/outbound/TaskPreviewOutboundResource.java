package io.github.balasis.taskmanager.context.web.resource.task.outbound;

import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserOutboundResource;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TaskPreviewOutboundResource {
    private String title;
    private String taskState;
    private Set<UserOutboundResource> assignees;
    private Set<UserOutboundResource> reviewers;
}
