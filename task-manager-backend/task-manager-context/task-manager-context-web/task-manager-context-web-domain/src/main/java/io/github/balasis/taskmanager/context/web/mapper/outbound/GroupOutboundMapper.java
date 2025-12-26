package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.web.resource.group.outbound.GroupOutboundResource;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.contracts.enums.BlobDefaultImageContainer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {UserOutboundMapper.class})
public interface GroupOutboundMapper extends BaseOutboundMapper<Group, GroupOutboundResource> {

    @Mapping(source = "imgUrl", target = "imgUrl", qualifiedByName = "convertGroupImgUrl")
    @Mapping(source = "defaultImgUrl", target = "defaultImgUrl", qualifiedByName = "convertGroupDefaultImgUrl")
    GroupOutboundResource toResource(Group group);

    @Named("convertGroupImgUrl")
    default String convertGroupImgUrl(String imgUrl) {
        if (imgUrl == null || imgUrl.isBlank()) {
            return null;
        }

        return "/blob/" + BlobContainerType.GROUP_IMAGES.getContainerName() + "/" + imgUrl;
    }

    @Named("convertGroupDefaultImgUrl")
    default String convertGroupDefaultImgUrl(String defaultImgUrl){
        if ((defaultImgUrl == null) || defaultImgUrl.isBlank()){
            return null;
        }

        return "/blob/"+ BlobDefaultImageContainer.GROUP_IMAGES.getContainerName()+"/"+defaultImgUrl;
    }
}
