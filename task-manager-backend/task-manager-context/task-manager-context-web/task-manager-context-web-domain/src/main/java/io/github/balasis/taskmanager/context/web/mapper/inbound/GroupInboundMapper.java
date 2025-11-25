package io.github.balasis.taskmanager.context.web.mapper.inbound;

import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.web.resource.group.inbound.GroupInboundResource;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {UserInboundMapper.class})
public interface GroupInboundMapper {
    Group toDomain(GroupInboundResource resource);
    List<Group> toDomains(List<GroupInboundResource> resources);
}
