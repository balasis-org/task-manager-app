package io.github.balasis.taskmanager.context.web.resource.user.outbound;


import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserMiniForDropdownOutboundResource extends BaseOutboundResource {
    private String email;
    private String name;
    private String defaultImgUrl;
    private String imgUrl;
    private Boolean sameOrg;
}
