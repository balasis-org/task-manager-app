package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserOutboundResource;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.contracts.enums.BlobDefaultImageContainer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserOutboundMapper extends BaseOutboundMapper<User, UserOutboundResource> {

    @Mapping(source = "imgUrl", target = "imgUrl", qualifiedByName = "convertUserImgUrl")
    @Mapping(source = "defaultImgUrl", target = "defaultImgUrl", qualifiedByName = "convertUserDefaultImgUrl")
    UserOutboundResource toResource(User user);


    @Named("convertUserImgUrl")
    default String convertUserImgUrl(String imgUrl){
        if ((imgUrl == null) || imgUrl.isBlank()){
            return null;
        }

        return BlobContainerType.PROFILE_IMAGES.getContainerName()+"/"+imgUrl;
    }

    @Named("convertUserDefaultImgUrl")
    default String convertUserDefaultImgUrl(String defaultImgUrl){
        if ((defaultImgUrl == null) || defaultImgUrl.isBlank()){
            return null;
        }

        return BlobDefaultImageContainer.PROFILE_IMAGES.getContainerName()+"/"+defaultImgUrl;
    }

}
