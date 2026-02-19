package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;
import io.github.balasis.taskmanager.context.base.enumeration.SystemRole;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.base.exception.authorization.InvalidRoleException;
import io.github.balasis.taskmanager.context.base.exception.business.BusinessRuleException;
import io.github.balasis.taskmanager.context.base.exception.notfound.EntityNotFoundException;
import io.github.balasis.taskmanager.context.base.exception.notfound.TaskFileNotFoundException;
import io.github.balasis.taskmanager.context.base.model.*;
import io.github.balasis.taskmanager.engine.core.repository.*;
import io.github.balasis.taskmanager.engine.core.transfer.TaskFileDownload;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;
import io.github.balasis.taskmanager.engine.infrastructure.blob.service.BlobStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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
    private final BlobStorageService blobStorageService;

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
    public Page<Group> listGroups(String q, Pageable pageable) {
        requireAdmin();
        if (q != null && !q.isBlank()) {
            return groupRepository.adminSearchGroups(q.trim(), pageable);
        }
        return groupRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Task> listTasks(String q, Pageable pageable) {
        requireAdmin();
        if (q != null && !q.isBlank()) {
            return taskRepository.adminSearchTasks(q.trim(), pageable);
        }
        return taskRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<TaskComment> listComments(Long taskId, Long groupId, Long creatorId, Pageable pageable) {
        requireAdmin();
        return taskCommentRepository.adminFilterComments(taskId, groupId, creatorId, pageable);
    }

    /* ━━━━ single-entity detail ━━━━ */

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        requireAdmin();
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public Group getGroup(Long groupId) {
        requireAdmin();
        return groupRepository.findByIdWithTasksAndParticipants(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));
    }

    @Transactional(readOnly = true)
    public Task getTask(Long taskId) {
        requireAdmin();
        return taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
    }

    @Transactional(readOnly = true)
    public TaskComment getComment(Long commentId) {
        requireAdmin();
        return taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));
    }

    /* ━━━━ file downloads ━━━━ */

    @Transactional(readOnly = true)
    public TaskFileDownload downloadTaskFile(Long taskId, Long fileId) {
        requireAdmin();
        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        TaskFile file = task.getCreatorFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new TaskFileNotFoundException("File not found"));
        byte[] data = blobStorageService.downloadTaskFile(file.getFileUrl());
        return new TaskFileDownload(data, file.getName());
    }

    @Transactional(readOnly = true)
    public TaskFileDownload downloadAssigneeTaskFile(Long taskId, Long fileId) {
        requireAdmin();
        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        TaskAssigneeFile file = task.getAssigneeFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new TaskFileNotFoundException("File not found"));
        byte[] data = blobStorageService.downloadTaskAssigneeFile(file.getFileUrl());
        return new TaskFileDownload(data, file.getName());
    }

    /* ━━━━ admin update — bypasses all group-role checks ━━━━ */

    @Transactional
    public User updateUser(Long userId, String name, String email, SystemRole systemRole,
                           SubscriptionPlan subscriptionPlan, Boolean allowEmailNotification) {
        requireAdmin();
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (name != null && !name.isBlank()) user.setName(name);
        if (email != null && !email.isBlank()) user.setEmail(email);
        if (systemRole != null) user.setSystemRole(systemRole);
        if (subscriptionPlan != null) user.setSubscriptionPlan(subscriptionPlan);
        if (allowEmailNotification != null) user.setAllowEmailNotification(allowEmailNotification);

        return userRepository.save(user);
    }

    @Transactional
    public Group updateGroup(Long groupId, String name, String description, String announcement,
                             Boolean allowEmailNotification) {
        requireAdmin();
        var group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        if (name != null && !name.isBlank()) {
            // check uniqueness for the same owner, excluding self
            boolean dup = groupRepository.existsByNameAndOwner_IdAndIdNot(name, group.getOwner().getId(), groupId);
            if (dup) throw new BusinessRuleException(
                    "The owner already has another group named '" + name + "'. Rename that group first.");
            group.setName(name);
        }
        if (description != null) group.setDescription(description);
        if (announcement != null) group.setAnnouncement(announcement);
        if (allowEmailNotification != null) group.setAllowEmailNotification(allowEmailNotification);

        return groupRepository.save(group);
    }

    @Transactional
    public Task updateTask(Long taskId, String title, String description,
                           TaskState taskState, Integer priority, Instant dueDate) {
        requireAdmin();
        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        if (title != null && !title.isBlank()) {
            if (taskRepository.existsByTitleAndIdNot(title, taskId)) {
                throw new BusinessRuleException(
                        "A task with this title already exists. Choose a different title.");
            }
            task.setTitle(title);
        }
        if (description != null && !description.isBlank()) task.setDescription(description);
        if (taskState != null) task.setTaskState(taskState);
        if (priority != null) task.setPriority(priority);
        if (dueDate != null) task.setDueDate(dueDate);

        task.setLastEditDate(Instant.now());
        return taskRepository.save(task);
    }

    @Transactional
    public TaskComment updateComment(Long commentId, String comment) {
        requireAdmin();
        var existing = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        if (comment != null && !comment.isBlank()) {
            existing.setComment(comment);
        }
        return taskCommentRepository.save(existing);
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
