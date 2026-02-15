package io.github.balasis.taskmanager.engine.core.service.authorization;


import io.github.balasis.taskmanager.context.base.enumeration.Role;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
public class RolePolicyService {

    private static final Set<Role> ALLOWED_REVIEWER_ROLES = EnumSet.of(
            Role.REVIEWER,
            Role.TASK_MANAGER,
            Role.GROUP_LEADER
    );

    private static final Set<Role> ALLOWED_ASSIGNEE_ROLES = EnumSet.of(
            Role.MEMBER,
            Role.REVIEWER,
            Role.TASK_MANAGER,
            Role.GROUP_LEADER
    );

    public boolean canBeReviewer(Role role) {
        return ALLOWED_REVIEWER_ROLES.contains(role);
    }

    public boolean canBeAssignee(Role role) {
        return ALLOWED_ASSIGNEE_ROLES.contains(role);
    }

    public Set<Role> getAllowedReviewerRoles() {
        return ALLOWED_REVIEWER_ROLES;
    }

    public Set<Role> getAllowedAssigneeRoles() {
        return ALLOWED_ASSIGNEE_ROLES;
    }
}
