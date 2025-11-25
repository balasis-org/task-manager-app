package io.github.balasis.taskmanager.engine.core.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.web.mapper.BaseOutboundMapper;
import io.github.balasis.taskmanager.context.web.resource.group.outbound.GroupOutboundResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserOutboundMapper.class})
public interface GroupOutboundMapper extends BaseOutboundMapper<Group, GroupOutboundResource> {
}
