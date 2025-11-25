package io.github.balasis.taskmanager.engine.core.mapper.inbound;

import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.context.web.resource.user.inbound.UserInboundResource;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserInboundMapper {
    User toDomain(UserInboundResource resource);
    List<User> toDomains(List<UserInboundResource> resources);
}
