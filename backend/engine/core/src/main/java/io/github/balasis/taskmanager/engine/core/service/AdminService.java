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

    private void requireAdmin() {
        var userId = effectiveCurrentUser.getUserId();
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (user.getSystemRole() != SystemRole.ADMIN) {
            throw new InvalidRoleException("Admin access required");
        }
    }

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
        return groupRepository.adminFindAllGroups(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Task> listTasks(String q, Pageable pageable) {
        requireAdmin();
        if (q != null && !q.isBlank()) {
            return taskRepository.adminSearchTasks(q.trim(), pageable);
        }
        return taskRepository.adminFindAllTasks(pageable);
    }

    @Transactional(readOnly = true)
    public Page<TaskComment> listComments(Long taskId, Long groupId, Long creatorId, Pageable pageable) {
        requireAdmin();
        return taskCommentRepository.adminFilterComments(taskId, groupId, creatorId, pageable);
    }

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        requireAdmin();
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public Group getGroup(Long groupId) {
        requireAdmin();
        return groupRepository.adminFindByIdWithDetails(groupId)
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
        return taskCommentRepository.findByIdWithTaskAndCreator(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));
    }

    @Transactional(readOnly = true)
    public TaskFileDownload downloadTaskFile(Long taskId, Long fileId) {
        requireAdmin();
        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        TaskFile file = task.getCreatorFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new TaskFileNotFoundException("File not found"));
        var download = blobStorageService.downloadTaskFile(file.getFileUrl());
        return new TaskFileDownload(download.inputStream(), file.getName(), download.size());
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
        var download = blobStorageService.downloadTaskAssigneeFile(file.getFileUrl());
        return new TaskFileDownload(download.inputStream(), file.getName(), download.size());
    }

    @Transactional
    public User updateUser(Long userId, String name, String email, SystemRole systemRole,
                           SubscriptionPlan subscriptionPlan, Boolean allowEmailNotification) {
        requireAdmin();
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (name != null && !name.isBlank()) user.setName(name);
        if (email != null && !email.isBlank()) user.setEmail(email);
        if (systemRole != null) user.setSystemRole(systemRole);
        if (subscriptionPlan != null) {
            user.setSubscriptionPlan(subscriptionPlan);
            // Bump lastChangeInGroup on all groups this user owns so that
            // the polling has-changed endpoint detects the plan change and
            // frontends refresh their cached plan-derived limits.
            groupRepository.touchLastChangeByOwnerId(userId, Instant.now());
        }
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

            boolean dup = groupRepository.existsByNameAndOwner_IdAndIdNot(name, group.getOwner().getId(), groupId);
            if (dup) throw new BusinessRuleException(
                    "Owner already has a group named '" + name + "', rename it first.");
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
            if (taskRepository.existsByTitleAndGroup_IdAndIdNot(title, task.getGroup().getId(), taskId)) {
                throw new BusinessRuleException(
                        "Duplicate task title in this group, pick a different one.");
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

    @Transactional
    public void deleteUser(Long userId) {
        requireAdmin();
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (user.getSystemRole() == SystemRole.ADMIN) {
            throw new BusinessRuleException(
                    "Can't delete an admin. Change the ADMIN-EMAIL env var to demote first.");
        }

        long ownedGroups = groupRepository.countByOwner_Id(userId);
        if (ownedGroups > 0) {
            throw new BusinessRuleException(
                    "User still owns " + ownedGroups + " group(s), delete those first.");
        }

        taskCommentRepository.detachCreatorFromAllComments(userId, user.getName());
        taskRepository.nullifyReviewedByForUser(userId);
        taskRepository.nullifyLastEditByForUser(userId);
        groupInvitationRepository.deleteAllByUser_IdOrInvitedBy_Id(userId, userId);
        refreshTokenRepository.deleteAllByUser_Id(userId);
        userRepository.delete(user);
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        requireAdmin();
        var group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        groupInvitationRepository.deleteAllByGroup_Id(groupId);

        deletedTaskRepository.deleteAllByGroup_Id(groupId);

        taskRepository.deleteAllByGroup_Id(groupId);

        groupMembershipRepository.deleteAllByGroup_Id(groupId);

        groupRepository.delete(group);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        requireAdmin();
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));

        taskRepository.delete(task);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        requireAdmin();
        var comment = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        taskCommentRepository.delete(comment);
    }

    @Transactional
    public User resetUserEmailUsage(Long userId) {
        requireAdmin();
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setUsedEmailsMonth(0);
        return userRepository.save(user);
    }

    @Transactional
    public User resetUserDownloadUsage(Long userId) {
        requireAdmin();
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setUsedDownloadBytesMonth(0L);
        return userRepository.save(user);
    }
}
