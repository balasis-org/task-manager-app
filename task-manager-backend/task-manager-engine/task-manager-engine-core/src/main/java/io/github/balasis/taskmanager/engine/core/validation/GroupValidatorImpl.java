package io.github.balasis.taskmanager.engine.core.validation;

import io.github.balasis.taskmanager.context.base.enumeration.TaskParticipantRole;
import io.github.balasis.taskmanager.context.base.exception.authorization.InvalidRoleException;
import io.github.balasis.taskmanager.context.base.exception.authorization.NotAGroupMemberException;
import io.github.balasis.taskmanager.context.base.exception.authorization.UnauthorizedException;
import io.github.balasis.taskmanager.context.base.exception.duplicate.GroupDuplicateException;
import io.github.balasis.taskmanager.context.base.exception.validation.InvalidFieldValueException;
import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.GroupMembership;
import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.engine.core.repository.GroupMembershipRepository;
import io.github.balasis.taskmanager.engine.core.repository.GroupRepository;
import io.github.balasis.taskmanager.engine.core.repository.TaskRepository;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.core.service.authorization.RolePolicyService;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GroupValidatorImpl implements GroupValidator{
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final EffectiveCurrentUser effectiveCurrentUser;
    private final RolePolicyService rolePolicyService;

    @Override
    public void validate(Group group) {

        boolean doesGroupExistWithSameName=groupRepository.existsByNameAndOwner_Id(
                group.getName(),effectiveCurrentUser.getUserId());
        if(doesGroupExistWithSameName){
            throw new GroupDuplicateException("You already have a group with that name");
        }
    }

    @Override
    public void validateForPatch(Long id, Group group) {
        if (group.getName() != null && group.getName().isBlank()) {
            throw new InvalidFieldValueException("Name cannot be empty");
        }
        boolean doesGroupExistWithSameName = groupRepository.existsByNameAndOwner_IdAndIdNot(
                group.getName(), effectiveCurrentUser.getUserId(),id);
        if (doesGroupExistWithSameName) {
            throw new GroupDuplicateException("You already have a group with that name");
        }
    }


    @Override
    public void validateForCreateTask(Long groupId, Set<Long> assignedIds, Set<Long> reviewerIds) {
        if (assignedIds != null && !assignedIds.isEmpty()) {

            for (Long assignedId : assignedIds){
                GroupMembership assignedMembership = groupMembershipRepository
                        .findByGroupIdAndUserId(groupId, assignedId)
                        .orElseThrow(() -> new NotAGroupMemberException("Some of the Assigned users are no longer part of the group"));

                if (!rolePolicyService.canBeAssignee(assignedMembership.getRole())) {
                    throw new InvalidRoleException(
                            "Assigned user must have one of the roles: " + rolePolicyService.getAllowedAssigneeRoles()
                    );
                }
            }
        }

        if (reviewerIds != null && !reviewerIds.isEmpty()) {
            for (Long reviewerId : reviewerIds){
                GroupMembership reviewerMembership = groupMembershipRepository
                        .findByGroupIdAndUserId(groupId, reviewerId)
                        .orElseThrow(() -> new NotAGroupMemberException("Some of the Reviewers are no longer part of the group"));

                if (!rolePolicyService.canBeReviewer(reviewerMembership.getRole())) {
                    throw new InvalidRoleException(
                            "Reviewer user must have one of the roles: " + rolePolicyService.getAllowedReviewerRoles()
                    );
                }
            }
        }

    }

    @Override
    public void validateForPatchTask(Long groupId, Long taskId, Task task) {
        if (task.getTitle() != null && task.getTitle().isBlank()){
            throw new InvalidFieldValueException("Task title cannot be empty");
        }
        if (task.getDescription() != null && task.getDescription().isBlank()){
            throw new InvalidFieldValueException("A minimum description is required");
        }
    }

    @Override
    public void validateAddAssigneeToTask(Task task, Long groupId, Long userId) {
        var membership = groupMembershipRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new NotAGroupMemberException("User to be set as assignee is not a member of the group"));

        if (!rolePolicyService.canBeAssignee(membership.getRole())) {
            throw new InvalidRoleException("User to be set as assignee has insufficient role. Allowed roles: "
                    + rolePolicyService.getAllowedAssigneeRoles());
        }

        if (!Objects.equals(task.getGroup().getId(), groupId)) {
            throw new UnauthorizedException("Task does not belong to the group given");
        }

        boolean alreadyAssigned = task.getTaskParticipants().stream()
                .anyMatch(tp -> tp.getTaskParticipantRole() == TaskParticipantRole.ASSIGNEE && tp.getUser().getId().equals(userId));
        if (alreadyAssigned) {
            throw new InvalidFieldValueException("User is already an assignee of this task");
        }
    }

    @Override
    public void validateAddReviewerToTask(Task task, Long groupId, Long userId) {
        var membership = groupMembershipRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new NotAGroupMemberException("User to be set as reviewer is not a member of the group"));

        if (!rolePolicyService.canBeReviewer(membership.getRole())) {
            throw new InvalidRoleException("User to be set as reviewer has insufficient role. Allowed roles: " + rolePolicyService.getAllowedReviewerRoles());
        }

        if (!Objects.equals(task.getGroup().getId(), groupId)) {
            throw new UnauthorizedException("Task does not belong to the group given");
        }

        boolean alreadyReviewer = task.getTaskParticipants().stream()
                .anyMatch(tp -> tp.getTaskParticipantRole() == TaskParticipantRole.REVIEWER && tp.getUser().getId().equals(userId));
        if (alreadyReviewer) {
            throw new InvalidFieldValueException("User is already a reviewer of this task");
        }
    }

    @Override
    public void validateAddTaskFile(Task task, Long groupId, MultipartFile file) {
        if (!Objects.equals(task.getGroup().getId(), groupId)) {
            throw new UnauthorizedException("Task does not belong to the group given");
        }
        if (file == null || file.isEmpty()) {
            throw new InvalidFieldValueException("File cannot be empty");
        }
    }

    @Override
    public void validateRemoveAssigneeFromTask(Task task, Long groupId, Long userId) {
        if (!Objects.equals(task.getGroup().getId(), groupId)) {
            throw new UnauthorizedException("Task does not belong to the group given");
        }

        boolean assigned = task.getTaskParticipants().stream()
                .anyMatch(tp -> tp.getTaskParticipantRole() == TaskParticipantRole.ASSIGNEE && tp.getUser().getId().equals(userId));
        if (!assigned) {
            throw new InvalidFieldValueException("User is not an assignee of this task");
        }
    }

    @Override
    public void validateRemoveReviewerFromTask(Task task, Long groupId, Long userId) {
        if (!Objects.equals(task.getGroup().getId(), groupId)) {
            throw new UnauthorizedException("Task does not belong to the group given");
        }

        boolean reviewer = task.getTaskParticipants().stream()
                .anyMatch(tp -> tp.getTaskParticipantRole() == TaskParticipantRole.REVIEWER && tp.getUser().getId().equals(userId));
        if (!reviewer) {
            throw new InvalidFieldValueException("User is not a reviewer of this task");
        }
    }

    @Override
    public void validateRemoveTaskFile(Task task, Long groupId, Long fileId) {
        if (!Objects.equals(task.getGroup().getId(), groupId)) {
            throw new UnauthorizedException("Task does not belong to the group given");
        }

        boolean fileExists = task.getFiles().stream().anyMatch(f -> f.getId().equals(fileId));
        if (!fileExists) {
            throw new InvalidFieldValueException("File does not exist in the task");
        }
    }

}
