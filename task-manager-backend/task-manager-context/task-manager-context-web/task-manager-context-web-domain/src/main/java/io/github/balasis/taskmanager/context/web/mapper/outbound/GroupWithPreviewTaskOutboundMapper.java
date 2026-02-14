package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.web.resource.group.outbound.GroupWithPreviewTasksOutboundResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring" , uses = {UserOutboundMapper.class, TaskPreviewOutboundMapper.class})
public interface GroupWithPreviewTaskOutboundMapper extends BaseOutboundMapper<Group, GroupWithPreviewTasksOutboundResource> {
}
