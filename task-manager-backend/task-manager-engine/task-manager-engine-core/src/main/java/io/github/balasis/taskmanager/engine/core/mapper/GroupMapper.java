package io.github.balasis.taskmanager.engine.core.mapper;

import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.web.mapper.BaseMapper;
import io.github.balasis.taskmanager.context.web.resource.GroupResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GroupMapper extends BaseMapper<Group, GroupResource> {
}
