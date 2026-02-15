package io.github.balasis.taskmanager.context.web.resource.task.outbound;

import io.github.balasis.taskmanager.context.base.enumeration.ReviewersDecision;

import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.taskfile.outbound.TaskFileOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.taskparticipant.outbound.TaskParticipantOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserOutboundResource;
import lombok.*;

import java.time.Instant;
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
    private Set<TaskParticipantOutboundResource> taskParticipants;
    private Set<TaskFileOutboundResource> files;
    private Set<TaskFileOutboundResource> assigneeFiles;
    private ReviewersDecision reviewersDecision;
    private UserOutboundResource reviewedBy;
    private String reviewComment;
    private UserOutboundResource lastEditBy;
    private Instant lastEditDate;
}
