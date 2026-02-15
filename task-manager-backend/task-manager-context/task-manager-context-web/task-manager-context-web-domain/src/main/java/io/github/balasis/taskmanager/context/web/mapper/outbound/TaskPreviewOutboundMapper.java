package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.web.resource.task.outbound.TaskPreviewOutboundResource;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring", uses = {TaskParticipantOutboundMapper.class})
public interface TaskPreviewOutboundMapper extends BaseOutboundMapper<Task, TaskPreviewOutboundResource>{
    TaskPreviewOutboundResource toResource(Task task);
}
