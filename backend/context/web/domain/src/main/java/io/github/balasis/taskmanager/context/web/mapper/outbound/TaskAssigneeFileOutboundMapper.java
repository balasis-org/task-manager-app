package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.TaskAssigneeFile;
import io.github.balasis.taskmanager.context.web.resource.taskfile.outbound.TaskFileOutboundResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskAssigneeFileOutboundMapper extends BaseOutboundMapper<TaskAssigneeFile, TaskFileOutboundResource> {

    @Mapping(source = "uploadedBy.name", target = "uploadedByName")
    @Mapping(target = "reviews", ignore = true)
    TaskFileOutboundResource toResource(TaskAssigneeFile taskAssigneeFile);
}
