package io.github.balasis.taskmanager.context.web.mapper.outbound;


import io.github.balasis.taskmanager.context.base.model.GroupMembership;
import io.github.balasis.taskmanager.context.web.resource.groupmembership.outbound.GroupMembershipOutboundResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserOutboundMapper.class})
public interface GroupMembershipOutboundMapper extends BaseOutboundMapper<GroupMembership, GroupMembershipOutboundResource> {
}
