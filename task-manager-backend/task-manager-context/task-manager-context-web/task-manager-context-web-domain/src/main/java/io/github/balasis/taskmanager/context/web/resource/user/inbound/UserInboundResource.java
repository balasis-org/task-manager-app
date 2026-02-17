package io.github.balasis.taskmanager.context.web.resource.user.inbound;

import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserInboundResource extends BaseInboundResource {
    @Size(max = 100, message = "name must be at most 100 characters")
    private String name;
    private Boolean allowEmailNotification;
}
