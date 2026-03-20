package io.github.balasis.taskmanager.engine.core.test;

import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.engine.core.service.authorization.RolePolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RolePolicyServiceTest {

    private RolePolicyService rolePolicyService;

    @BeforeEach
    void setUp() {
        rolePolicyService = new RolePolicyService();
    }

    // reviewer eligibility

    @Test
    void canBeReviewer_guestIsNotAllowed() {
        assertFalse(checkReviewer(Role.GUEST));
    }

    @Test
    void canBeReviewer_memberIsNotAllowed() {
        assertFalse(checkReviewer(Role.MEMBER));
    }

    @Test
    void canBeReviewer_reviewerIsAllowed() {
        assertTrue(checkReviewer(Role.REVIEWER));
    }

    @Test
    void canBeReviewer_taskManagerIsAllowed() {
        assertTrue(checkReviewer(Role.TASK_MANAGER));
    }

    @Test
    void canBeReviewer_groupLeaderIsAllowed() {
        assertTrue(checkReviewer(Role.GROUP_LEADER));
    }

    // assignee eligibility

    @Test
    void canBeAssignee_guestIsNotAllowed() {
        assertFalse(checkAssignee(Role.GUEST));
    }

    @Test
    void canBeAssignee_memberIsAllowed() {
        assertTrue(checkAssignee(Role.MEMBER));
    }

    @Test
    void canBeAssignee_reviewerIsAllowed() {
        assertTrue(checkAssignee(Role.REVIEWER));
    }

    @Test
    void canBeAssignee_taskManagerIsAllowed() {
        assertTrue(checkAssignee(Role.TASK_MANAGER));
    }

    @Test
    void canBeAssignee_groupLeaderIsAllowed() {
        assertTrue(checkAssignee(Role.GROUP_LEADER));
    }

    // role sets

    @Test
    void getAllowedReviewerRoles_containsExactlyThreeRoles() {
        Set<Role> reviewerRoles = rolePolicyService.getAllowedReviewerRoles();
        assertEquals(3, reviewerRoles.size());
        assertContainsAll(reviewerRoles, Role.REVIEWER, Role.TASK_MANAGER, Role.GROUP_LEADER);
    }

    @Test
    void getAllowedAssigneeRoles_containsExactlyFourRoles() {
        Set<Role> assigneeRoles = rolePolicyService.getAllowedAssigneeRoles();
        assertEquals(4, assigneeRoles.size());
        assertContainsAll(assigneeRoles, Role.MEMBER, Role.REVIEWER, Role.TASK_MANAGER, Role.GROUP_LEADER);
    }

    @Test
    void reviewerRolesAreSubsetOfAssigneeRoles() {
        Set<Role> reviewerRoles = rolePolicyService.getAllowedReviewerRoles();
        Set<Role> assigneeRoles = rolePolicyService.getAllowedAssigneeRoles();
        assertTrue(assigneeRoles.containsAll(reviewerRoles),
                "every reviewer should also be eligible as an assignee");
    }

    // private helpers

    private boolean checkReviewer(Role role) {
        return rolePolicyService.canBeReviewer(role);
    }

    private boolean checkAssignee(Role role) {
        return rolePolicyService.canBeAssignee(role);
    }

    private void assertContainsAll(Set<Role> actualSet, Role... expectedRoles) {
        for (Role role : expectedRoles) {
            assertTrue(actualSet.contains(role), "expected set to contain " + role);
        }
    }
}
