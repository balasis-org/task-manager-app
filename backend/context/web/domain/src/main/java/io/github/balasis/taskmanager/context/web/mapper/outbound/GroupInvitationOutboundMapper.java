package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.GroupInvitation;
import io.github.balasis.taskmanager.context.web.resource.groupinvitation.outbound.GroupInvitationOutboundResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

// pulls groupName from the nested Group entity so the frontend doesn't
// need a separate group lookup just to show the invitation card.
@Mapper(componentModel = "spring" , uses = {UserOutboundMapper.class})
public interface GroupInvitationOutboundMapper extends BaseOutboundMapper<GroupInvitation, GroupInvitationOutboundResource>{

    @Override
    @Mapping(target = "groupName" ,source = "group.name")
    GroupInvitationOutboundResource toResource(GroupInvitation groupInvitation);
}
