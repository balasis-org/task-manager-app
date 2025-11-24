package io.github.balasis.taskmanager.engine.core.mapper;

import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.context.web.mapper.BaseMapper;
import io.github.balasis.taskmanager.context.web.resource.UserResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper extends BaseMapper<User, UserResource> {
}
