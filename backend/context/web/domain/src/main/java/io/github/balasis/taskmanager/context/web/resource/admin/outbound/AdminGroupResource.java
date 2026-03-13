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
public class AdminGroupResource extends BaseOutboundResource {
    private String name;
    private String description;
    private String ownerName;
    private String ownerEmail;
    private int memberCount;
    private int taskCount;
    private Instant createdAt;
    private String announcement;
    private Boolean allowEmailNotification;
    private Instant lastGroupEventDate;
    private List<AdminGroupMemberResource> members;
}
