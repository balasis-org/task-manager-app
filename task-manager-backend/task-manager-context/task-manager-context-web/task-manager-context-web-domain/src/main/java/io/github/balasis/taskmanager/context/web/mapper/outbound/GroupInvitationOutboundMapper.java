package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.GroupInvitation;
import io.github.balasis.taskmanager.context.web.resource.groupinvitation.outbound.GroupInvitationOutboundResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring" , uses = {UserOutboundMapper.class})
public interface GroupInvitationOutboundMapper extends BaseOutboundMapper<GroupInvitation, GroupInvitationOutboundResource>{

    @Override
    @Mapping(target = "groupName" ,source = "group.name")
    GroupInvitationOutboundResource toResource(GroupInvitation groupInvitation);
}
