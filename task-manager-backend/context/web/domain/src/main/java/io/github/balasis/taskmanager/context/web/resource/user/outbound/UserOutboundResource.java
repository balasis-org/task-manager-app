package io.github.balasis.taskmanager.context.web.resource.user.outbound;

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
public class UserOutboundResource extends BaseOutboundResource {
    private String email;
    private String name;
    private String defaultImgUrl;
    private String imgUrl;
    private String msProfilePhotoUrl;
    private Instant lastSeenInvites;
    private Boolean allowEmailNotification;
    private String cacheKey;
    private String inviteCode;
    private Boolean sameOrg;
    private SystemRole systemRole;
    private SubscriptionPlan subscriptionPlan;
    private Long usedStorageBytes;
    private Long usedDownloadBytesMonth;
    /** Populated at the controller level — PlanLimits budget for this user's plan. */
    private Long storageBudgetBytes;
    /** Populated at the controller level — max groups this user may own. */
    private Integer maxGroups;
    /** Populated at the controller level — monthly download budget in bytes. */
    private Long downloadBudgetBytes;
    /** Populated at the controller level — max members per group for this plan. */
    private Integer maxMembersPerGroup;
    private Integer usedImageScansMonth;
    /** Populated at the controller level — monthly image scan cap for this plan. */
    private Integer imageScansPerMonth;
    private Integer usedEmailsMonth;
    /** Populated at the controller level — monthly email quota for this plan. */
    private Integer emailsPerMonth;
}
