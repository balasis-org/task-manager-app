package io.github.balasis.taskmanager.engine.core.validation;

import io.github.balasis.taskmanager.context.base.exception.authorization.InvalidRoleException;
import io.github.balasis.taskmanager.context.base.exception.authorization.NotAGroupMemberException;
import io.github.balasis.taskmanager.context.base.exception.duplicate.GroupDuplicateException;
import io.github.balasis.taskmanager.context.base.exception.validation.InvalidFieldValueException;
import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.GroupMembership;
import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.engine.core.repository.GroupMembershipRepository;
import io.github.balasis.taskmanager.engine.core.repository.GroupRepository;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.core.service.authorization.RolePolicyService;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class GroupValidatorImpl implements GroupValidator{
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final EffectiveCurrentUser effectiveCurrentUser;
    private final RolePolicyService rolePolicyService;

    @Override
    public void validate(Group group) {

        boolean doesGroupExistWithSameName=groupRepository.existsByNameAndOwner_Id(
                group.getName(),effectiveCurrentUser.getUserId());
        if(doesGroupExistWithSameName){
            throw new GroupDuplicateException("You already have a group with that name");
        };
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


}
