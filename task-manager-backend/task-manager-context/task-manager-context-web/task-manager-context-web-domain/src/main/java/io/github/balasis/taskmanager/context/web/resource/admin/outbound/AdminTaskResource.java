package io.github.balasis.taskmanager.context.web.resource.admin.outbound;

import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AdminTaskResource extends BaseOutboundResource {
    private String title;
    private String taskState;
    private Integer priority;
    private Long groupId;
    private String groupName;
    private String creatorNameSnapshot;
    private Long commentCount;
    private Instant createdAt;
    private Instant dueDate;
    private String description;
    private String reviewersDecision;
    private String reviewComment;
    private String reviewedBy;
    private List<AdminTaskParticipantResource> participants;
    private List<AdminTaskFileResource> creatorFiles;
    private List<AdminTaskFileResource> assigneeFiles;
}
