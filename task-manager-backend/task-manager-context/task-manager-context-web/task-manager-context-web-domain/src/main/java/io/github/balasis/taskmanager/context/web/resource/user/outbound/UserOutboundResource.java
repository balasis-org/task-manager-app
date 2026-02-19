package io.github.balasis.taskmanager.context.web.resource.user.outbound;

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
    private Instant lastSeenInvites;
    private Boolean allowEmailNotification;
    private String cacheKey;
    private String inviteCode;
    private Boolean sameOrg;
    private SystemRole systemRole;
}
