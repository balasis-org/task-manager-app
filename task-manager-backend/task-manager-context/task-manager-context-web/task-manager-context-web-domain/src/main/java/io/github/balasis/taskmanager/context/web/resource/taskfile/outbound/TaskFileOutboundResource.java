package io.github.balasis.taskmanager.context.web.resource.taskfile.outbound;

import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import lombok.*;


@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TaskFileOutboundResource extends BaseOutboundResource {
    private String name;
}
