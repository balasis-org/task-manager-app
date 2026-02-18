package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.enumeration.SystemRole;
import io.github.balasis.taskmanager.context.base.exception.authorization.InvalidRoleException;
import io.github.balasis.taskmanager.context.base.exception.business.BusinessRuleException;
import io.github.balasis.taskmanager.context.base.exception.notfound.EntityNotFoundException;
import io.github.balasis.taskmanager.context.base.model.*;
import io.github.balasis.taskmanager.engine.core.repository.*;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final EffectiveCurrentUser effectiveCurrentUser;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupInvitationRepository groupInvitationRepository;
    private final GroupEventRepository groupEventRepository;
    private final DeletedTaskRepository deletedTaskRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TaskParticipantRepository taskParticipantRepository;

    /* ━━━━ guard ━━━━ */

    private void requireAdmin() {
        var userId = effectiveCurrentUser.getUserId();
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (user.getSystemRole() != SystemRole.ADMIN) {
            throw new InvalidRoleException("Admin access required");
        }
    }

    /* ━━━━ listings ━━━━ */

    @Transactional(readOnly = true)
    public Page<User> listUsers(String q, Pageable pageable) {
        requireAdmin();
        return userRepository.searchUser(q, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Group> listGroups(Pageable pageable) {
        requireAdmin();
        return groupRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Task> listTasks(Pageable pageable) {
        requireAdmin();
        return taskRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<TaskComment> listComments(Pageable pageable) {
        requireAdmin();
        return taskCommentRepository.findAll(pageable);
    }

    /* ━━━━ delete user ━━━━ */

    @Transactional
    public void deleteUser(Long userId) {
        requireAdmin();
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (user.getSystemRole() == SystemRole.ADMIN) {
            throw new BusinessRuleException(
                    "Cannot delete an admin user. Demote the user first by changing the ADMIN_EMAIL env variable.");
        }

        // check if user owns any groups — those must be deleted first
        long ownedGroups = groupRepository.findAll().stream()
                .filter(g -> g.getOwner().getId().equals(userId))
                .count();
        if (ownedGroups > 0) {
            throw new BusinessRuleException(
                    "Cannot delete user: they own " + ownedGroups + " group(s). Delete those groups first.");
        }

        // 1. detach comments (set creator=null, preserve snapshot)
        taskCommentRepository.detachCreatorFromAllComments(userId, user.getName());

        // 2. nullify task references (reviewedBy, lastEditBy)
        taskRepository.nullifyReviewedByForUser(userId);
        taskRepository.nullifyLastEditByForUser(userId);

        // 3. delete invitations (both sent and received)
        groupInvitationRepository.deleteAllByUser_IdOrInvitedBy_Id(userId, userId);

        // 4. delete refresh tokens
        refreshTokenRepository.deleteAllByUser_Id(userId);

        // 5. delete user (cascades: memberships, taskParticipants)
        userRepository.delete(user);
    }

    /* ━━━━ delete group ━━━━ */

    @Transactional
    public void deleteGroup(Long groupId) {
        requireAdmin();
        var group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        // 1. delete invitations for this group
        groupInvitationRepository.deleteAllByGroup_Id(groupId);

        // 2. delete deleted-task records
        deletedTaskRepository.deleteAllByGroup_Id(groupId);

        // 3. delete tasks (cascades: participants, files, comments)
        taskRepository.deleteAllByGroup_Id(groupId);

        // 4. delete memberships
        groupMembershipRepository.deleteAllByGroup_Id(groupId);

        // 5. delete the group itself (cascades: groupEvents)
        groupRepository.delete(group);
    }

    /* ━━━━ delete task ━━━━ */

    @Transactional
    public void deleteTask(Long taskId) {
        requireAdmin();
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        // cascade handles participants, files, comments
        taskRepository.delete(task);
    }

    /* ━━━━ delete comment ━━━━ */

    @Transactional
    public void deleteComment(Long commentId) {
        requireAdmin();
        var comment = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        taskCommentRepository.delete(comment);
    }
}
