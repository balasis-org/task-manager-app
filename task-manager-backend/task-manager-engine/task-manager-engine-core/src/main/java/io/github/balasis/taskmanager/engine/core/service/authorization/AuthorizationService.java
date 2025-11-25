package io.github.balasis.taskmanager.engine.core.service.authorization;

import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.exception.authorization.InvalidRoleException;
import io.github.balasis.taskmanager.context.base.exception.authorization.NotAGroupMemberException;
import io.github.balasis.taskmanager.engine.core.repository.GroupMembershipRepository;
import io.github.balasis.taskmanager.engine.infrastructure.auth.jwt.CurrentUser;
import io.github.balasis.taskmanager.engine.infrastructure.auth.jwt.EffectiveCurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final EffectiveCurrentUser effectiveCurrentUser;
    private final GroupMembershipRepository membershipRepo;

    public void requireRoleIn(Long groupId, Set<Role> allowedRoles) {
        var userId = effectiveCurrentUser.getUserId();

        var membership = membershipRepo
                .findByUserIdAndGroupId(userId,groupId)
                .orElseThrow(() -> new NotAGroupMemberException("Not a member of this group"));

        if (!allowedRoles.contains(membership.getRole())) {
            throw new InvalidRoleException("Insufficient role");
        }
    }
}

