package io.github.balasis.taskmanager.context.web.resource.admin.outbound;

import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AdminCommentResource extends BaseOutboundResource {
    private String comment;
    private String creatorName;
    private String creatorEmail;
    private Long creatorId;
    private Long taskId;
    private String taskTitle;
    private Long groupId;
    private String groupName;
    private Instant createdAt;
}
