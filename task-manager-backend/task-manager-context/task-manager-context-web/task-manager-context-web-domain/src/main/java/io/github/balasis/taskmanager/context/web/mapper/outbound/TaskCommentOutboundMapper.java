package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.TaskComment;
import io.github.balasis.taskmanager.context.web.resource.taskcomment.outbound.TaskCommentOutboundResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring" , uses = {UserOutboundMapper.class})
public interface TaskCommentOutboundMapper extends BaseOutboundMapper<TaskComment, TaskCommentOutboundResource> {
}
