package io.github.balasis.taskmanager.engine.core.service;


import io.github.balasis.taskmanager.context.base.enumeration.InvitationStatus;
import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.enumeration.TaskParticipantRole;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.base.model.*;
import io.github.balasis.taskmanager.engine.core.dto.GroupWithPreviewDto;
import io.github.balasis.taskmanager.engine.core.dto.TaskPreviewDto;
import io.github.balasis.taskmanager.context.base.model.TaskComment;
import io.github.balasis.taskmanager.engine.core.transfer.TaskFileDownload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Set;

public interface GroupService{
    //Group
    Group create(Group group);
    Group patch(Long groupId, Group group);
    void delete(Long groupId);
    Set<Group> findAllByCurrentUser();
    Group updateGroupImage(Long groupId, MultipartFile file);

    //GroupMemberships
    Page<GroupMembership> getAllGroupMembers(Long groupId, Pageable pageable);
    Page<GroupMembership> searchGroupMembers(Long groupId, String q, Pageable pageable);
    void removeGroupMember(Long groupId, Long groupMembershipId);

    GroupMembership changeGroupMembershipRole(Long groupId, Long groupMembershipId, Role newRole);

    //GroupInvitations
    GroupInvitation createGroupInvitation(Long groupId, Long userToBeInvited , Role roleOfUserToBeInvited, String comment);
    GroupInvitation respondToInvitation(Long invitationId, InvitationStatus status);
    Set<GroupInvitation> findMyGroupInvitations();
    Set<GroupInvitation> findInvitationsSentByMe();

    //GroupTasks
    Task createTask(Long groupId, Task task, Set<Long> assignedIds, Set<Long> reviewerIds, Set<MultipartFile> files);
    Task patchTask(Long groupId ,Long taskId, Task task);
    Task reviewTask(Long groupId, Long taskId, Task task);
    Task getTask(Long groupId, Long taskId);
    Set<Task> findMyTasks(Long groupId, Boolean reviewer, Boolean assigned, TaskState taskState);

    GroupWithPreviewDto findGroupWithPreviewTasks(Long groupId);

    //TaskComments
    Page<TaskComment> findAllTaskComments(Long groupId, Long taskId, Pageable pageable);
    TaskComment addTaskComment(Long groupId, Long taskId, String comment);
    TaskComment patchTaskComment(Long groupId, Long taskId, Long commentId, String comment);
    void deleteTaskComment(Long groupId, Long taskId, Long commentId);

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

    //Group Tasks Content
    Task addTaskParticipant(Long groupId, Long taskId , Long userId, TaskParticipantRole taskParticipantRole);
    void removeTaskParticipant(Long groupId, Long taskId, Long taskParticipantId);
    Task addTaskFile(Long groupId, Long taskId, MultipartFile file);
    void removeTaskFile(Long groupId, Long taskId, Long fileId);
    TaskFileDownload downloadTaskFile(Long groupId, Long taskId, Long fileId);

    Task addAssigneeTaskFile(Long groupId, Long taskId, MultipartFile file);
    void removeAssigneeTaskFile(Long groupId, Long taskId, Long fileId);
    TaskFileDownload downloadAssigneeTaskFile(Long groupId, Long taskId, Long fileId);

    Task markTaskToBeReviewed(Long groupId, Long taskId);
    Page<GroupEvent> findAllGroupEvents(Long groupId, Pageable pageable);

}
