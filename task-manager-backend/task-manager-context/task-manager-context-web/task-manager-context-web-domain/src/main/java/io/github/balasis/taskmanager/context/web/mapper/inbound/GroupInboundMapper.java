package io.github.balasis.taskmanager.context.web.mapper.inbound;

import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.web.resource.group.inbound.GroupInboundPatchResource;
import io.github.balasis.taskmanager.context.web.resource.group.inbound.GroupInboundResource;
import org.mapstruct.Mapper;

import java.util.Set;

@Mapper(componentModel = "spring", uses = {UserInboundMapper.class})
public interface GroupInboundMapper {
    Group toDomain(GroupInboundResource resource);
    Group toDomain(GroupInboundPatchResource resource);
    Set<Group> toDomains(Set<GroupInboundResource> resources);
}
