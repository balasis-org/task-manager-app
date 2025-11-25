package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.web.resource.task.outbound.TaskOutboundResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskOutboundMapper extends BaseOutboundMapper<Task, TaskOutboundResource> {
}
