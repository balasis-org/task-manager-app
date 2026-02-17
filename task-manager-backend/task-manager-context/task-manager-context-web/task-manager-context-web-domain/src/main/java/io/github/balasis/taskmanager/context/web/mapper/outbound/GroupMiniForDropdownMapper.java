package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.web.resource.group.outbound.GroupMiniForDropdownResource;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.contracts.enums.BlobDefaultImageContainer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface GroupMiniForDropdownMapper extends BaseOutboundMapper<Group, GroupMiniForDropdownResource> {

    @Mapping(source = "imgUrl", target = "imgUrl", qualifiedByName = "convertMiniGroupImgUrl")
    @Mapping(source = "defaultImgUrl", target = "defaultImgUrl", qualifiedByName = "convertMiniGroupDefaultImgUrl")
    GroupMiniForDropdownResource toResource(Group group);

    @Named("convertMiniGroupImgUrl")
    default String convertMiniGroupImgUrl(String imgUrl) {
        if (imgUrl == null || imgUrl.isBlank()) return null;
        return BlobContainerType.GROUP_IMAGES.getContainerName() + "/" + imgUrl;
    }

    @Named("convertMiniGroupDefaultImgUrl")
    default String convertMiniGroupDefaultImgUrl(String defaultImgUrl) {
        if (defaultImgUrl == null || defaultImgUrl.isBlank()) return null;
        return BlobDefaultImageContainer.GROUP_IMAGES.getContainerName() + "/" + defaultImgUrl;
    }
}
