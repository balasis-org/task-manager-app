package io.github.balasis.taskmanager.engine.core.validation;

import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.GroupInvitation;
import io.github.balasis.taskmanager.context.base.model.Task;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public interface GroupValidator extends BaseValidator<Group>{
    void validateForCreateTask(Long groupId, Set<Long> assignedIds, Set<Long> reviewerIds);

    void validateForPatchTask(Long groupId,Long taskId, Task task);

    void validateAddAssigneeToTask(Task task, Long groupId, Long userId);

    void validateRemoveAssigneeFromTask(Task task, Long groupId, Long userId);

    void validateAddReviewerToTask(Task task, Long groupId, Long userId);

    void validateRemoveReviewerFromTask(Task task, Long groupId, Long userId);

    void validateAddTaskFile(Task task, Long groupId, MultipartFile file);

    void validateRemoveTaskFile(Task task, Long groupId, Long fileId);

    void validateCreateGroupInvitation(GroupInvitation groupInvitation);

    void validateAcceptGroupInvitation(GroupInvitation groupInvitation);
}
