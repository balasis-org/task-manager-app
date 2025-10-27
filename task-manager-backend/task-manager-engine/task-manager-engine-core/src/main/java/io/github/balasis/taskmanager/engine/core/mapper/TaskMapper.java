package io.github.balasis.taskmanager.engine.core.mapper;


import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.web.mapper.BaseMapper;
import io.github.balasis.taskmanager.context.web.resource.TaskResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskMapper extends BaseMapper<Task, TaskResource> {
}
