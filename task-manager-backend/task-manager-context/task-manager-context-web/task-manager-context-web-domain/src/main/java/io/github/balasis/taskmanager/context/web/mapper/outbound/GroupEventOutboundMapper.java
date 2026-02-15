package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.GroupEvent;
import io.github.balasis.taskmanager.context.web.resource.groupevent.outbound.GroupEventOutboundResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring" , uses = {GroupOutboundMapper.class})
public interface GroupEventOutboundMapper extends BaseOutboundMapper<GroupEvent, GroupEventOutboundResource> {
}
