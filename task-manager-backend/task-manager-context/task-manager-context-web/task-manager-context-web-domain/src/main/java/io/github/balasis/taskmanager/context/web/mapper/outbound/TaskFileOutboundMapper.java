package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.TaskFile;
import io.github.balasis.taskmanager.context.web.resource.taskfile.outbound.TaskFileOutboundResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskFileOutboundMapper extends BaseOutboundMapper<TaskFile, TaskFileOutboundResource>{
}
