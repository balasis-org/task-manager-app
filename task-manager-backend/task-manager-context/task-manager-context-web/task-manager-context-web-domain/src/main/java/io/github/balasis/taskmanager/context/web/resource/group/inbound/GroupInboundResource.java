package io.github.balasis.taskmanager.context.web.resource.group.inbound;

import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GroupInboundResource extends BaseInboundResource {

    @NotBlank(message = "name is mandatory")
    private String name;
    private String description;
}
