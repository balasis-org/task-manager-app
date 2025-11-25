package io.github.balasis.taskmanager.engine.core.validation;

import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.exception.authorization.InvalidRoleException;
import io.github.balasis.taskmanager.context.base.exception.authorization.NotAGroupMemberException;
import io.github.balasis.taskmanager.context.base.exception.duplicate.GroupDuplicateException;
import io.github.balasis.taskmanager.context.base.exception.notfound.UserNotFoundException;
import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.GroupMembership;
import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.engine.core.repository.GroupMembershipRepository;
import io.github.balasis.taskmanager.engine.core.repository.GroupRepository;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.core.service.authorization.AuthorizationService;
import io.github.balasis.taskmanager.engine.core.service.authorization.RolePolicyService;
import io.github.balasis.taskmanager.engine.infrastructure.auth.jwt.EffectiveCurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

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
    public void validateForUpdate(Long id, Group group) {


    }


    @Override
    public void validateForCreateTask(Long groupId, Long assignedId, Long reviewerId) {
        if (assignedId != null) {
            GroupMembership assignedMembership = groupMembershipRepository
                    .findByGroupIdAndUserId(groupId, assignedId)
                    .orElseThrow(() -> new NotAGroupMemberException("Assigned user is not a member of the group"));

            if (!rolePolicyService.canBeAssignee(assignedMembership.getRole())) {
                throw new InvalidRoleException(
                        "Assigned user must have one of the roles: " + rolePolicyService.getAllowedAssigneeRoles()
                );
            }
        }

        if (reviewerId != null) {
            GroupMembership reviewerMembership = groupMembershipRepository
                    .findByGroupIdAndUserId(groupId, reviewerId)
                    .orElseThrow(() -> new NotAGroupMemberException("Reviewer is not a member of the group"));

            if (!rolePolicyService.canBeReviewer(reviewerMembership.getRole())) {
                throw new InvalidRoleException(
                        "Reviewer user must have one of the roles: " + rolePolicyService.getAllowedReviewerRoles()
                );
            }
        }
    }

}
