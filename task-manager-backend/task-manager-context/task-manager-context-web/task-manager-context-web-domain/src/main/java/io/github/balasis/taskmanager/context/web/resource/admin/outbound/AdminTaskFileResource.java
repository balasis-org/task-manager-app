package io.github.balasis.taskmanager.context.web.resource.admin.outbound;

import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AdminTaskFileResource extends BaseOutboundResource {
    private String name;
    private String fileUrl;
}
