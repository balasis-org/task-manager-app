package io.github.balasis.taskmanager.context.web.resource.group.outbound;

import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserOutboundResource;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GroupOutboundResource extends BaseOutboundResource {
    private String name;
    private String description;
    private String defaultImgUrl;
    private String imgUrl;
    private UserOutboundResource owner;
    private String Announcement;
    private Instant createdAt;
    private Instant lastGroupEventDate;
    private Boolean allowEmailNotification;
}
