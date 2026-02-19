package io.github.balasis.taskmanager.context.web.resource.group.inbound;

import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GroupInboundPatchResource extends BaseInboundResource {
    @Size(max = 50, message = "name must be at most 50 characters")
    private String name;

    @Size(max = 500, message = "description must be at most 500 characters")
    private String description;

    @Size(max = 150, message = "announcement must be at most 150 characters")
    private String Announcement;
    private Boolean allowEmailNotification;
}
