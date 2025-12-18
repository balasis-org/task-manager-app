package io.github.balasis.taskmanager.context.web.resource.task.outbound;

import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.taskfile.outbound.TaskFileOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserOutboundResource;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TaskOutboundResource extends BaseOutboundResource {
    private String title;
    private String description;
    private String taskState;
    private UserOutboundResource creator;
    private Set<UserOutboundResource> assignees;
    private Set<UserOutboundResource> reviewers;
    private Set<TaskFileOutboundResource> files;
}
