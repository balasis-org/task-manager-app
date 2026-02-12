package io.github.balasis.taskmanager.engine.core.validation;

import io.github.balasis.taskmanager.context.base.model.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.Set;

public interface GroupValidator extends BaseValidator<Group>{
    void validateForCreateTask(Long groupId, Set<Long> assignedIds, Set<Long> reviewerIds);

    void validateForPatchTask(Long groupId,Long taskId, Task task);

    void validateAddAssigneeToTask(Task task, Long groupId, Long userId);

    void validateAddReviewerToTask(Task task, Long groupId, Long userId);

    void validateRemoveTaskParticipant(Task task, Long groupId ,Long taskParticipantId);

    void validateAddTaskFile(Task task, Long groupId, MultipartFile file);
    void validateDownloadTaskFile(Task task, Long groupId);

    void validateRemoveTaskFile(Task task, Long groupId, Long fileId);

    void validateCreateGroupInvitation(GroupInvitation groupInvitation);

    void validateAcceptGroupInvitation(GroupInvitation groupInvitation);

    void validateRemoveGroupMember(Long groupId, Long currentUserId, Long memberUserId, Optional<GroupMembership> currentMembershipOpt);

}
