package io.github.balasis.taskmanager.engine.core.service.authorization;

import io.github.balasis.taskmanager.context.base.enumeration.Role;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

// defines which roles can be assigned to task participant positions.
// this is the single source of truth for role eligibility — the validator
// and GroupServiceImpl both delegate here instead of hardcoding role checks.
// MEMBER can be an assignee but not a reviewer; GUEST can be neither.
@Service
public class RolePolicyService {

    // reviewers need at least REVIEWER rank; MEMBER and GUEST cant review
    private static final Set<Role> ALLOWED_REVIEWER_ROLES = EnumSet.of(
            Role.REVIEWER,
            Role.TASK_MANAGER,
            Role.GROUP_LEADER
    );

    // assignees can be anyone except GUEST
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
