package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.TaskParticipant;
import io.github.balasis.taskmanager.context.web.resource.taskparticipant.outbound.TaskParticipantOutboundResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring" , uses = {UserOutboundMapper.class})
public interface TaskParticipantOutboundMapper extends BaseOutboundMapper<TaskParticipant,TaskParticipantOutboundResource>{
}
