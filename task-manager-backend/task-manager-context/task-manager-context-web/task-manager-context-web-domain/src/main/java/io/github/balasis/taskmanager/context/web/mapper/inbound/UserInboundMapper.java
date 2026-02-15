package io.github.balasis.taskmanager.context.web.mapper.inbound;

import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.context.web.resource.user.inbound.UserInboundResource;
import org.mapstruct.Mapper;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserInboundMapper {
    User toDomain(UserInboundResource resource);
    Set<User> toDomains(Set<UserInboundResource> resources);
}
