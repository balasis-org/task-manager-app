package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.model.*;
import io.github.balasis.taskmanager.context.web.resource.admin.outbound.*;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.contracts.enums.BlobDefaultImageContainer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

// admin-specific mapper with list vs detail variants: list ignores heavy nested
// fields (members, description) for page performance, detail includes them.
// URL converters prepend the blob container name so the frontend can build
// the full SAS URL — same pattern used in GroupOutboundMapper/UserOutboundMapper.
@Mapper(componentModel = "spring")
public interface AdminOutboundMapper {

    @Mapping(source = "org", target = "isOrg")
    @Mapping(source = "imgUrl", target = "imgUrl", qualifiedByName = "convertUserImgUrl")
    @Mapping(source = "defaultImgUrl", target = "defaultImgUrl", qualifiedByName = "convertUserDefaultImgUrl")
    @Mapping(target = "storageBudgetBytes", ignore = true)
    @Mapping(target = "downloadBudgetBytes", ignore = true)
    @Mapping(target = "emailsPerMonth", ignore = true)
    @Mapping(target = "imageScansPerMonth", ignore = true)
    @Mapping(target = "taskAnalysisCreditsPerMonth", ignore = true)
    AdminUserResource toUserResource(User user);

    @Named("groupList")
    @Mapping(source = "owner.name", target = "ownerName")
    @Mapping(source = "owner.email", target = "ownerEmail")
    @Mapping(target = "memberCount", expression = "java(group.getMemberships() != null ? group.getMemberships().size() : 0)")
    @Mapping(target = "taskCount", expression = "java(group.getTasks() != null ? group.getTasks().size() : 0)")
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "announcement", ignore = true)
    @Mapping(target = "allowEmailNotification", ignore = true)
    @Mapping(target = "lastGroupEventDate", ignore = true)
    AdminGroupResource toGroupListResource(Group group);

    @Named("groupDetail")
    @Mapping(source = "owner.name", target = "ownerName")
    @Mapping(source = "owner.email", target = "ownerEmail")
    @Mapping(target = "memberCount", expression = "java(group.getMemberships() != null ? group.getMemberships().size() : 0)")
    @Mapping(target = "taskCount", expression = "java(group.getTasks() != null ? group.getTasks().size() : 0)")
    @Mapping(source = "memberships", target = "members")
    AdminGroupResource toGroupDetailResource(Group group);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    AdminGroupMemberResource toGroupMemberResource(GroupMembership membership);

    @Named("taskList")
    @Mapping(source = "group.id", target = "groupId")
    @Mapping(source = "group.name", target = "groupName")
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "reviewersDecision", ignore = true)
    @Mapping(target = "reviewComment", ignore = true)
    @Mapping(target = "reviewedBy", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "creatorFiles", ignore = true)
    @Mapping(target = "assigneeFiles", ignore = true)
    AdminTaskResource toTaskListResource(Task task);

    @Named("taskDetail")
    @Mapping(source = "group.id", target = "groupId")
    @Mapping(source = "group.name", target = "groupName")
    @Mapping(target = "reviewedBy", expression = "java(task.getReviewedBy() != null ? task.getReviewedBy().getName() : null)")
    @Mapping(source = "taskParticipants", target = "participants")
    AdminTaskResource toTaskDetailResource(Task task);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "taskParticipantRole", target = "role")
    AdminTaskParticipantResource toTaskParticipantResource(TaskParticipant participant);

    @Mapping(source = "fileUrl", target = "fileUrl", qualifiedByName = "convertTaskFileUrl")
    AdminTaskFileResource toCreatorFileResource(TaskFile file);

    @Mapping(source = "fileUrl", target = "fileUrl", qualifiedByName = "convertAssigneeFileUrl")
    AdminTaskFileResource toAssigneeFileResource(TaskAssigneeFile file);

    @Mapping(target = "creatorName", expression = "java(comment.getCreator() != null ? comment.getCreator().getName() : comment.getCreatorNameSnapshot())")
    @Mapping(target = "creatorEmail", expression = "java(comment.getCreator() != null ? comment.getCreator().getEmail() : null)")
    @Mapping(target = "creatorId", expression = "java(comment.getCreator() != null ? comment.getCreator().getId() : null)")
    @Mapping(source = "task.id", target = "taskId")
    @Mapping(source = "task.title", target = "taskTitle")
    @Mapping(target = "groupId", expression = "java(comment.getTask() != null && comment.getTask().getGroup() != null ? comment.getTask().getGroup().getId() : null)")
    @Mapping(target = "groupName", expression = "java(comment.getTask() != null && comment.getTask().getGroup() != null ? comment.getTask().getGroup().getName() : null)")
    AdminCommentResource toCommentResource(TaskComment comment);

    @Named("convertUserImgUrl")
    default String convertUserImgUrl(String imgUrl) {
        if (imgUrl == null || imgUrl.isBlank()) return null;
        return BlobContainerType.PROFILE_IMAGES.getContainerName() + "/" + imgUrl;
    }

    @Named("convertUserDefaultImgUrl")
    default String convertUserDefaultImgUrl(String defaultImgUrl) {
        if (defaultImgUrl == null || defaultImgUrl.isBlank()) return null;
        return BlobDefaultImageContainer.PROFILE_IMAGES.getContainerName() + "/" + defaultImgUrl;
    }

    @Named("convertTaskFileUrl")
    default String convertTaskFileUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return null;
        return BlobContainerType.TASK_FILES.getContainerName() + "/" + fileUrl;
    }

    @Named("convertAssigneeFileUrl")
    default String convertAssigneeFileUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return null;
        return BlobContainerType.TASK_ASSIGNEE_FILES.getContainerName() + "/" + fileUrl;
    }
}
