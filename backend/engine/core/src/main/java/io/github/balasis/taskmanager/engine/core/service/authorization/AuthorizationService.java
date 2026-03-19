package io.github.balasis.taskmanager.engine.core.service.authorization;

import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.exception.authorization.InvalidRoleException;
import io.github.balasis.taskmanager.context.base.exception.authorization.NotAGroupMemberException;
import io.github.balasis.taskmanager.context.base.model.GroupMembership;
import io.github.balasis.taskmanager.engine.core.repository.GroupMembershipRepository;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

// central gate for group-level authorization. every controller endpoint that
// touches a group calls one of these before doing anything. the idea is to keep
// role checks out of the service layer so GroupServiceImpl doesnt have to care
// about who the caller is — it just does business logic.
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final EffectiveCurrentUser effectiveCurrentUser;
    private final GroupMembershipRepository membershipRepo;

    // strict version: caller must be a member AND have one of the allowed roles
    public void requireRoleIn(Long groupId, Set<Role> allowedRoles) {
        var userId = effectiveCurrentUser.getUserId();

        var membership = membershipRepo
                .findByUserIdAndGroupId(userId,groupId)
                .orElseThrow(() -> new NotAGroupMemberException("Not a member of this group"));

        if (!allowedRoles.contains(membership.getRole())) {
            throw new InvalidRoleException("Insufficient role");
        }
    }

    // loose version: caller just needs to be a member, any role is fine
    public void requireAnyRoleIn(Long groupId){
        var userId = effectiveCurrentUser.getUserId();
        membershipRepo.findByUserIdAndGroupId(userId,groupId)
                .orElseThrow(() -> new NotAGroupMemberException("Not a member of this group"));
    }

    // same as above but returns the membership so callers that need
    // the Role later dont have to re-query
    public GroupMembership requireAnyRoleInAndGet(Long groupId) {
        var userId = effectiveCurrentUser.getUserId();
        return membershipRepo.findByUserIdAndGroupId(userId, groupId)
                .orElseThrow(() -> new NotAGroupMemberException("Not a member of this group"));
    }

}
