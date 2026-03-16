package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.web.resource.task.outbound.TaskOutboundResource;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {TaskFileOutboundMapper.class, TaskAssigneeFileOutboundMapper.class, TaskParticipantOutboundMapper.class, UserOutboundMapper.class})
public interface TaskOutboundMapper extends BaseOutboundMapper<Task, TaskOutboundResource> {

    @Mapping(source = "creatorFiles", target = "files")
    @Mapping(target = "effectiveMaxCreatorFiles", ignore = true)
    @Mapping(target = "effectiveMaxAssigneeFiles", ignore = true)
    @Mapping(target = "effectiveMaxFileSizeBytes", ignore = true)
    TaskOutboundResource toResource(Task task);

}
