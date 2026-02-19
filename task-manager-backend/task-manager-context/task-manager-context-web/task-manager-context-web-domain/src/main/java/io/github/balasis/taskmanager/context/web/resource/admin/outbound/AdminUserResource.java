package io.github.balasis.taskmanager.context.web.resource.admin.outbound;

import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;
import io.github.balasis.taskmanager.context.base.enumeration.SystemRole;
import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserResource extends BaseOutboundResource {
    private String email;
    private String name;
    private SystemRole systemRole;
    private SubscriptionPlan subscriptionPlan;
    private Boolean isOrg;
    private Instant lastActiveAt;
    private Boolean allowEmailNotification;
    private String tenantId;
    private String imgUrl;
    private String defaultImgUrl;
    private Instant lastSeenInvites;
    private Instant lastInviteReceivedAt;
}
