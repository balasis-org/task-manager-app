package io.github.balasis.taskmanager.context.web.resource.group.outbound;

import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupMiniForDropdownResource extends BaseOutboundResource {
    private String defaultImgUrl;
    private String imgUrl;
    private String name;
}
