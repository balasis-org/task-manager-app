package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.enumeration.FileReviewDecision;
import io.github.balasis.taskmanager.context.base.enumeration.AnalysisType;
import io.github.balasis.taskmanager.context.base.enumeration.InvitationStatus;
import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.enumeration.TaskParticipantRole;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.base.model.*;
import io.github.balasis.taskmanager.engine.core.dto.AnalysisEstimateDto;
import io.github.balasis.taskmanager.engine.core.dto.EffectiveFileLimitsDto;
import io.github.balasis.taskmanager.engine.core.dto.FileReviewInfoDto;
import io.github.balasis.taskmanager.engine.core.dto.GroupFileDto;
import io.github.balasis.taskmanager.engine.core.dto.GroupRefreshDto;
import io.github.balasis.taskmanager.engine.core.dto.GroupWithPreviewDto;
import io.github.balasis.taskmanager.engine.core.dto.TaskPreviewDto;
import io.github.balasis.taskmanager.context.base.model.TaskComment;
import io.github.balasis.taskmanager.engine.core.transfer.TaskFileDownload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

// core service contract. everything group-related goes through here:
// group CRUD, membership, invitations, tasks, task files, comments,
// polling/refresh, file reviews, and comment intelligence.
// implementation is in GroupServiceImpl.
public interface GroupService{

    Group create(Group group);
    Group patch(Long groupId, Group group);
    void delete(Long groupId);
    Set<Group> findAllByCurrentUser();
    Group updateGroupImage(Long groupId, MultipartFile file);
    Group pickDefaultGroupImage(Long groupId, String fileName);

    Page<GroupMembership> getAllGroupMembers(Long groupId, Pageable pageable);
    Page<GroupMembership> searchGroupMembers(Long groupId, String q, Pageable pageable);
    void removeGroupMember(Long groupId, Long groupMembershipId);

    GroupMembership changeGroupMembershipRole(Long groupId, Long groupMembershipId, Role newRole);

    void createGroupInvitation(Long groupId, String inviteCode, Role roleOfUserToBeInvited, String comment, boolean sendEmail);
    GroupInvitation respondToInvitation(Long invitationId, InvitationStatus status);
    Set<GroupInvitation> findMyGroupInvitations();
    Set<GroupInvitation> findInvitationsSentByMe();

    Task createTask(Long groupId, Task task, Set<Long> assignedIds, Set<Long> reviewerIds, Set<MultipartFile> files);
    Task patchTask(Long groupId ,Long taskId, Task task);
    Task reviewTask(Long groupId, Long taskId, Task task);
    Task getTask(Long groupId, Long taskId);
    Set<Task> findMyTasks(Long groupId, Boolean reviewer, Boolean assigned, TaskState taskState);

    GroupWithPreviewDto findGroupWithPreviewTasks(Long groupId);

    Page<TaskComment> findAllTaskComments(Long groupId, Long taskId, Pageable pageable);
    TaskComment addTaskComment(Long groupId, Long taskId, String comment);
    TaskComment patchTaskComment(Long groupId, Long taskId, Long commentId, String comment);
    void deleteTaskComment(Long groupId, Long taskId, Long commentId);
    int bulkDeleteCommentsBefore(Long groupId, Long taskId, java.time.Instant before);

        Set<TaskPreviewDto> findTasksWithPreviewByFilters(
            Long groupId,
            Long creatorId,
            Boolean creatorIsMe,
            Long reviewerId,
            Boolean reviewerIsMe,
            Long assigneeId,
            Boolean assigneeIsMe,
            Instant dueDateBefore
        );

    Task addTaskParticipant(Long groupId, Long taskId , Long userId, TaskParticipantRole taskParticipantRole);
    void removeTaskParticipant(Long groupId, Long taskId, Long taskParticipantId);
    void notifyTaskParticipant(Long groupId, Long taskId, Long userId, String customNote);
    void notifyTaskParticipants(Long groupId, Long taskId, Set<Long> userIds, String customNote);
    Task addTaskFile(Long groupId, Long taskId, MultipartFile file);
    void removeTaskFile(Long groupId, Long taskId, Long fileId);
    TaskFileDownload downloadTaskFile(Long groupId, Long taskId, Long fileId);

    Task addAssigneeTaskFile(Long groupId, Long taskId, MultipartFile file);
    void removeAssigneeTaskFile(Long groupId, Long taskId, Long fileId);
    TaskFileDownload downloadAssigneeTaskFile(Long groupId, Long taskId, Long fileId);

    Task markTaskToBeReviewed(Long groupId, Long taskId);
    Page<GroupEvent> findAllGroupEvents(Long groupId, Pageable pageable);
    void deleteAllGroupEvents(Long groupId);

    // refresh returns only the parts that changed since the client's
    // last-seen timestamp, keeping polling payloads small
    GroupRefreshDto refreshGroup(Long groupId, Instant lastSeen);

    void deleteTask(Long groupId, Long taskId);

    Set<Long> findAccessibleTaskIds(Long groupId);

    Set<Long> findFilteredTaskIds(
        Long groupId,
        Long creatorId,
        Boolean creatorIsMe,
        Long reviewerId,
        Boolean reviewerIsMe,
        Long assigneeId,
        Boolean assigneeIsMe,
        Instant dueDateBefore,
        Integer priorityMin,
        Integer priorityMax,
        TaskState taskState,
        Boolean hasFiles
    );

    // lightweight has-changed checks: the frontend calls these frequently
    // (every few seconds) to decide whether it needs to call the heavier
    // refresh/detail endpoints.
    boolean hasNewInvitations();

    boolean hasGroupChanged(Long groupId, Instant lastSeen);
    boolean hasTaskChanged(Long groupId, Long taskId, Instant since);
    boolean hasCommentsChanged(Long groupId, Long taskId, Instant since);

    // Lightweight membership gate — throws if the caller is not a member
    void checkMembership(Long groupId);

    // ── Comment Intelligence (Teams Pro only) ─────────────────
    // AI-driven analysis of task comment threads. estimate shows the
    // credit cost before the user commits; requestAnalysis actually
    // queues the work; getAnalysisSnapshot returns the results.

    AnalysisEstimateDto getAnalysisEstimate(Long groupId, Long taskId);

    int requestAnalysis(Long groupId, Long taskId, AnalysisType type);

    TaskAnalysisSnapshot getAnalysisSnapshot(Long groupId, Long taskId);

    // ── File Gallery & Per-File Review ──────────────────────────    // browse all files across all tasks in one place,
    // plus per-file approve/reject by reviewers
    List<GroupFileDto> getGroupFiles(Long groupId);

    void reviewTaskFile(Long groupId, Long taskId, Long fileId, FileReviewDecision status, String note);

    void reviewAssigneeFile(Long groupId, Long taskId, Long fileId, FileReviewDecision status, String note);

    // Batch-fetch review info for a set of file IDs (keyed by file id)
    Map<Long, List<FileReviewInfoDto>> getFileReviews(Set<Long> creatorFileIds, Set<Long> assigneeFileIds);

    // Resolve effective file limits for a task (task → group → plan, Math.min)
    EffectiveFileLimitsDto resolveEffectiveFileLimits(Long groupId, Task task);
}
