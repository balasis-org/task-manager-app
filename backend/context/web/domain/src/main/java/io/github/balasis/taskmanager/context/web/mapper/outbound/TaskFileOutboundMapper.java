package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.TaskFile;
import io.github.balasis.taskmanager.context.web.resource.taskfile.outbound.TaskFileOutboundResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

// reviews ignored here — enriched in GroupController.enrichFileReviews()
// after a separate batch query to avoid N+1.
@Mapper(componentModel = "spring")
public interface TaskFileOutboundMapper extends BaseOutboundMapper<TaskFile, TaskFileOutboundResource>{

    @Mapping(source = "uploadedBy.name", target = "uploadedByName")
    @Mapping(target = "reviews", ignore = true)
    TaskFileOutboundResource toResource(TaskFile taskFile);
}
