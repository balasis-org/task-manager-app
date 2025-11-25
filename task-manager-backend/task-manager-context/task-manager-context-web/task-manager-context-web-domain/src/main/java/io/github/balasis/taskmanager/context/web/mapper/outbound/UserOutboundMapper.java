package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.context.web.mapper.BaseOutboundMapper;
import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserOutboundResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserOutboundMapper extends BaseOutboundMapper<User, UserOutboundResource> {
}
