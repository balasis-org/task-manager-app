package io.github.balasis.taskmanager.context.web.mapper.inbound;

import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.web.resource.task.inbound.TaskInboundResource;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TaskInboundMapper {
    Task toDomain(TaskInboundResource resource);
    List<Task> toDomains(List<TaskInboundResource> resources);

}
