package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.engine.core.dto.GroupWithPreviewDto;
import io.github.balasis.taskmanager.engine.core.dto.TaskPreviewDto;
import io.github.balasis.taskmanager.context.base.enumeration.InvitationStatus;
import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.enumeration.TaskParticipantRole;
import io.github.balasis.taskmanager.context.base.enumeration.ReviewersDecision;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.base.exception.authorization.NotAGroupMemberException;
import io.github.balasis.taskmanager.context.base.exception.business.InvalidMembershipRemovalException;
import io.github.balasis.taskmanager.context.base.exception.notfound.*;
import io.github.balasis.taskmanager.context.base.exception.validation.InvalidFieldValueException;
import io.github.balasis.taskmanager.context.base.model.*;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.engine.core.repository.*;
import io.github.balasis.taskmanager.engine.core.service.authorization.AuthorizationService;
import io.github.balasis.taskmanager.engine.core.transfer.TaskFileDownload;
import io.github.balasis.taskmanager.engine.core.validation.GroupValidator;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;
import io.github.balasis.taskmanager.engine.infrastructure.blob.service.BlobStorageService;
import io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
public class GroupServiceImpl extends BaseComponent implements GroupService{
    private final GroupRepository groupRepository;
    private final GroupValidator groupValidator;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskParticipantRepository taskParticipantRepository;
    private final TaskFileRepository taskFileRepository;
    private final TaskAssigneeFileRepository taskAssigneeFileRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupEventRepository groupEventRepository;
    private final EffectiveCurrentUser effectiveCurrentUser;
    private final ObjectProvider<EmailClient> emailClientProvider;
    private final BlobStorageService blobStorageService;
    private final AuthorizationService authorizationService;
    private final DefaultImageService defaultImageService;
    private final GroupInvitationRepository groupInvitationRepository;


    private Instant touchLastGroupEventDate(Group group) {
        Instant now = Instant.now();
        group.setLastGroupEventDate(now);
        return now;
    }


    @Override
    public Group create(Group group){
        var user = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(()->new UserNotFoundException("User not found"));
        groupValidator.validate(group);
        group.setOwner(user);
        group.setDefaultImgUrl(defaultImageService.pickRandom(BlobContainerType.GROUP_IMAGES));
        Group savedGroup = groupRepository.save(group);
        groupMembershipRepository.save(
                GroupMembership.builder()
                        .user(user)
                        .group(savedGroup)
                        .role(Role.GROUP_LEADER)
                        .build()
        );
        return savedGroup;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Group> findAllByCurrentUser() {
        Long userId = effectiveCurrentUser.getUserId();
        return groupMembershipRepository.findByUserIdWithGroup(userId)
                .stream()
                .map(GroupMembership::getGroup)
                .collect(Collectors.toSet());
    }


    @Override
    public Group patch(Long groupId, Group group) {
        authorizationService.requireRoleIn(groupId,Set.of(Role.GROUP_LEADER));
        groupValidator.validateForPatch(groupId, group);
        Group existingGroup = groupRepository.findById(groupId)
                .orElseThrow(()->new GroupNotFoundException("Group with id:"+groupId +" doesn't exist"));

        if (group.getName() != null) {
            String previousName = existingGroup.getName();
            existingGroup.setName(group.getName());
            touchLastGroupEventDate(existingGroup);
            var gEvName = GroupEvent.builder().group(existingGroup)
                    .description("Group has been renamed from " + previousName +
                            " to " + group.getName()).build();
            groupEventRepository.save(gEvName);
        }
        if (group.getDescription() != null) {
            existingGroup.setDescription(group.getDescription());
            touchLastGroupEventDate(existingGroup);
            var gEvDesc =  GroupEvent.builder().group(existingGroup)
                    .description("Group Description has been changed")
                    .build();
            groupEventRepository.save(gEvDesc);
        }
        if (group.getAnnouncement() != null) {
            existingGroup.setAnnouncement(group.getAnnouncement());
            touchLastGroupEventDate(existingGroup);
            var gEvAnn =  GroupEvent.builder().group(existingGroup)
                    .description("Group Announcement has been changed")
                    .build();
            groupEventRepository.save(gEvAnn);
        }

        if (group.getAllowEmailNotification() != null) {
            existingGroup.setAllowEmailNotification(group.getAllowEmailNotification());
            touchLastGroupEventDate(existingGroup);
            var gEvAllowEmail =  GroupEvent.builder().group(existingGroup)
                    .description(  (group.getAllowEmailNotification()
                    ? "Group email notifications are enabled"
                    : "Group email notifications are disabled"))
                    .build();
            groupEventRepository.save(gEvAllowEmail);
        }



        return groupRepository.save(existingGroup);
    }

    @Override
    public void delete(Long groupId) {
        authorizationService.requireRoleIn(groupId,Set.of(Role.GROUP_LEADER));
        taskRepository.deleteAllByGroup_Id(groupId);
        groupMembershipRepository.deleteAllByGroup_Id(groupId);
        groupRepository.deleteById(groupId);
    }



    @Override
    public Group updateGroupImage(Long groupId, MultipartFile file) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with id " + groupId + " not found"));

        String blobName = blobStorageService.uploadGroupImage(file, groupId);
        group.setImgUrl(blobName);

        return groupRepository.save(group);
    }

    @Override
    public Page<GroupMembership> getAllGroupMembers(Long groupId, Pageable pageable) {
        authorizationService.requireAnyRoleIn(groupId);
        return groupMembershipRepository.findByGroup_Id(groupId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GroupMembership> searchGroupMembers(Long groupId, String q, Pageable pageable) {
        authorizationService.requireAnyRoleIn(groupId);
        String normalized = (q == null || q.isBlank()) ? null : q.trim();
        return groupMembershipRepository.searchByGroupIdAndUser(groupId, normalized, pageable);
    }

    @Override
    public Page<GroupEvent> findAllGroupEvents(Long groupId, Pageable pageable) {
        authorizationService.requireAnyRoleIn(groupId);
        GroupMembership requestersMembership = groupMembershipRepository.findByUserIdAndGroupId(effectiveCurrentUser.getUserId(),groupId)
                .orElseThrow(() -> new EntityNotFoundException("Requester not found in the group"));
        requestersMembership.setLastSeenGroupEvents(Instant.now());
        groupMembershipRepository.save(requestersMembership);
        return groupEventRepository.findAllByGroup_Id(groupId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
        public GroupWithPreviewDto findGroupWithPreviewTasks(Long groupId) {
        authorizationService.requireAnyRoleIn(groupId);
        Group group = groupRepository.findByIdWithTasksAndParticipants(groupId)
            .orElseThrow(() -> new GroupNotFoundException("Group with id " + groupId + " not found"));

        Long currentUserId = effectiveCurrentUser.getUserId();
        var membershipOpt = groupMembershipRepository.findByGroupIdAndUserId(groupId, currentUserId);
        boolean isLeaderOrManager = membershipOpt.isPresent() && (
            membershipOpt.get().getRole() == Role.GROUP_LEADER ||
            membershipOpt.get().getRole() == Role.TASK_MANAGER
        );

        Set<TaskPreviewDto> previews = group.getTasks().stream()
            .map(task -> {
                boolean accessible = isLeaderOrManager || task.getTaskParticipants().stream()
                    .anyMatch(tp -> tp.getUser().getId().equals(currentUserId));
                boolean areThereNewCommentsToBeRead = task.getTaskParticipants().stream()
                        .anyMatch(tp -> tp.getUser().getId().equals(currentUserId)
                                &&  tp.getLastSeenTaskComments()!=null && task.getLastCommentDate()!=null
                                && tp.getLastSeenTaskComments().isBefore(task.getLastCommentDate()) );

                return TaskPreviewDto.builder()
                    .id(task.getId())
                    .title(task.getTitle())
                    .taskState(task.getTaskState())
                    .createdAt(task.getCreatedAt())
                    .dueDate(task.getDueDate())
                    .commentCount(task.getCommentCount())
                    .accessible(accessible)
                    .newCommentsToBeRead(areThereNewCommentsToBeRead)
                    .build();
            })
            .collect(Collectors.toSet());

        return GroupWithPreviewDto.builder()
            .id(group.getId())
            .name(group.getName())
            .description(group.getDescription())
            .defaultImgUrl(group.getDefaultImgUrl())
            .imgUrl(group.getImgUrl())
            .ownerId(group.getOwner().getId())
            .ownerName(group.getOwner().getName())
            .announcement(group.getAnnouncement())
            .createdAt(group.getCreatedAt())
            .taskPreviews(previews)
            .build();
        }

    @Override
    @Transactional(readOnly = true)
    public Set<TaskPreviewDto> findTasksWithPreviewByFilters(
            Long groupId,
            Long creatorId,
            Boolean creatorIsMe,
            Long reviewerId,
            Boolean reviewerIsMe,
            Long assigneeId,
            Boolean assigneeIsMe,
            Instant dueDateBefore
    ) {
        authorizationService.requireAnyRoleIn(groupId);
        Long currentUserId = effectiveCurrentUser.getUserId();
        var membershipOpt = groupMembershipRepository.findByGroupIdAndUserId(groupId, currentUserId);
        boolean isLeaderOrManager = membershipOpt.isPresent() && (
                membershipOpt.get().getRole() == Role.GROUP_LEADER ||
                membershipOpt.get().getRole() == Role.TASK_MANAGER
        );

        Long effectiveCreatorId = Boolean.TRUE.equals(creatorIsMe) ? currentUserId : creatorId;
        Long effectiveReviewerId = Boolean.TRUE.equals(reviewerIsMe) ? currentUserId : reviewerId;
        Long effectiveAssigneeId = Boolean.TRUE.equals(assigneeIsMe) ? currentUserId : assigneeId;

        boolean hasFilters =
                effectiveCreatorId != null ||
                effectiveReviewerId != null ||
                effectiveAssigneeId != null ||
                dueDateBefore != null;
            // ensure group exists and keep behavior consistent with other methods
            groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with id " + groupId + " not found"));

            if (!hasFilters) {
                Group group = groupRepository.findByIdWithTasksAndParticipants(groupId)
                    .orElseThrow(() -> new GroupNotFoundException("Group with id " + groupId + " not found"));

                return group.getTasks().stream()
                    .map(task -> {
                    boolean accessible = isLeaderOrManager || task.getTaskParticipants().stream()
                        .anyMatch(tp -> tp.getUser().getId().equals(currentUserId));

                    boolean areThereNewCommentsToBeRead = task.getTaskParticipants().stream()
                        .anyMatch(tp -> tp.getUser().getId().equals(currentUserId)
                            && tp.getLastSeenTaskComments() != null
                            && task.getLastCommentDate() != null
                            && tp.getLastSeenTaskComments().isBefore(task.getLastCommentDate()));

                    return TaskPreviewDto.builder()
                        .id(task.getId())
                        .title(task.getTitle())
                        .taskState(task.getTaskState())
                        .createdAt(task.getCreatedAt())
                        .dueDate(task.getDueDate())
                        .commentCount(task.getCommentCount())
                        .accessible(accessible)
                        .newCommentsToBeRead(areThereNewCommentsToBeRead)
                        .build();
                    })
                    .collect(Collectors.toSet());
            }

            Long participantUserId = isLeaderOrManager ? null : currentUserId;

            Set<Task> tasks = taskRepository.searchTasksForPreviewWithFilters(
                groupId,
                effectiveCreatorId,
                effectiveReviewerId,
                effectiveAssigneeId,
                participantUserId,
                dueDateBefore
            );

            return tasks.stream()
                .map(task -> {
                    boolean accessible = isLeaderOrManager || task.getTaskParticipants().stream()
                        .anyMatch(tp -> tp.getUser().getId().equals(currentUserId));

                    boolean areThereNewCommentsToBeRead = task.getTaskParticipants().stream()
                        .anyMatch(tp -> tp.getUser().getId().equals(currentUserId)
                            && tp.getLastSeenTaskComments() != null
                            && task.getLastCommentDate() != null
                            && tp.getLastSeenTaskComments().isBefore(task.getLastCommentDate()));

                    return TaskPreviewDto.builder()
                        .id(task.getId())
                        .title(task.getTitle())
                        .taskState(task.getTaskState())
                        .createdAt(task.getCreatedAt())
                        .dueDate(task.getDueDate())
                        .commentCount(task.getCommentCount())
                        .accessible(accessible)
                        .newCommentsToBeRead(areThereNewCommentsToBeRead)
                        .build();
                })
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public Page<TaskComment> findAllTaskComments(Long groupId, Long taskId ,Pageable pageable){
        var taskParticipant = taskParticipantRepository.findAllByTask_idAndUser_id(taskId,effectiveCurrentUser.getUserId());
        if (taskParticipant!=null){
            taskParticipant.setLastSeenTaskComments(Instant.now());
            taskParticipantRepository.save(taskParticipant);
        }
        return taskCommentRepository.findAllByTask_id(taskId,pageable);
    }


    @Override
    @Transactional
    public void removeGroupMember(Long groupId, Long groupMembershipId) {
        Long currentUserId = effectiveCurrentUser.getUserId();

        var currentMembershipOpt = groupMembershipRepository.findByGroupIdAndUserId(groupId, currentUserId);
        if (currentMembershipOpt.isEmpty()) {
            throw new NotAGroupMemberException("Not a member of this group");
        }
        var targetMembershipOpt = groupMembershipRepository.findById(groupMembershipId);
        if (targetMembershipOpt.isEmpty()) {
            throw new InvalidMembershipRemovalException("Membership to remove not found");
        }
        Long targetsId = targetMembershipOpt.get().getUser().getId();

        groupValidator.validateRemoveGroupMember(groupId,effectiveCurrentUser.getUserId(), targetsId
                ,currentMembershipOpt);

        var targetsName = userRepository.findById(targetsId).orElseThrow().getName();
        GroupEvent ge;
        Group gr = groupRepository.getReferenceById(groupId);
        touchLastGroupEventDate(gr);

        ge = GroupEvent.builder()
                .group(gr)
                .description(
                        Objects.equals(effectiveCurrentUser.getUserId(), targetsId)
                                ? targetsName + " has left the group"
                                : targetsName + " has been removed from the group"
                )
                .build();
        groupEventRepository.save(ge);
        taskParticipantRepository.deleteByUserIdAndGroupId(targetsId, groupId);
        groupMembershipRepository.deleteByGroupIdAndUserId(groupId, targetsId);
    }

    @Override
    public GroupMembership changeGroupMembershipRole(Long groupId, Long groupMembershipId, Role newRole) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER));

        var targetOpt = groupMembershipRepository.findById(groupMembershipId);
        if (targetOpt.isEmpty()) {
            throw new GroupMembershipNotFoundException("Target membership not found");
        }
        GroupMembership target = targetOpt.get();
        groupValidator.validateChangeGroupMembershipRole(groupId, target.getUser().getId(), newRole);

        if (newRole == Role.GROUP_LEADER) {
            groupMembershipRepository.findByGroup_IdAndRole(groupId, Role.GROUP_LEADER)
                    .ifPresent(existingLeader -> {
                        if (!Objects.equals(existingLeader.getId(), target.getId())) {
                            existingLeader.setRole(Role.TASK_MANAGER);
                            groupMembershipRepository.save(existingLeader);
                        }
                    });
        }

        if (newRole != Role.GROUP_LEADER && newRole != Role.TASK_MANAGER && newRole != Role.REVIEWER) {
            taskParticipantRepository.deleteReviewersByUserIdAndGroupId(target.getUser().getId(), groupId);
        }
        target.setRole(newRole);
        GroupMembership saved = groupMembershipRepository.save(target);
        // evict cache for this group to keep member listings fresh
        // Note: @CacheEvict on this method would be ideal; using CacheEvict annotation requires method-level placement.
        return saved;
    }


    @Override
    public GroupInvitation createGroupInvitation(Long groupId, Long userToBeInvited , Role userToBeInvitedRole, String comment){
        authorizationService.requireRoleIn(groupId,Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));

        var group = groupRepository.findById(groupId).orElseThrow();
        var targetUser = userRepository.findById(userToBeInvited)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        var inviter = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Inviter not found"));

        var groupInvitation = GroupInvitation.builder()
                .user(userRepository.getReferenceById(userToBeInvited))
                .invitationStatus(InvitationStatus.PENDING)
                .invitedBy(userRepository.getReferenceById(effectiveCurrentUser.getUserId()))
                .group(group)
                .userToBeInvitedRole( (userToBeInvitedRole == null) ? Role.MEMBER : userToBeInvitedRole)
            .comment((comment == null || comment.isBlank()) ? null : comment.trim())
                .build();
        groupValidator.validateCreateGroupInvitation(groupInvitation);

        GroupInvitation saved = groupInvitationRepository.save(groupInvitation);

        boolean groupAllows = Boolean.TRUE.equals(group.getAllowEmailNotification());
        boolean userAllows = Boolean.TRUE.equals(targetUser.getAllowEmailNotification());

        if (groupAllows && userAllows && targetUser.getEmail() != null && !targetUser.getEmail().isBlank()) {
            EmailClient emailClient = emailClientProvider.getIfAvailable();
            if (emailClient != null) {
                String subject = "Group invitation: " + group.getName();
                String body = "You have been invited to join the group '" + group.getName() + "' by " + inviter.getName() + ".";
                if (saved.getComment() != null && !saved.getComment().isBlank()) {
                    body += "\n\nInviters comment: " + saved.getComment();
                }
                emailClient.sendEmail(targetUser.getEmail(), subject, body);
            }
        }

        return saved;
    }

        @Override
        public GroupInvitation respondToInvitation(Long groupInvitationId, InvitationStatus status) {
        var groupInvitation = groupInvitationRepository.findByIdWithGroup(groupInvitationId)
            .orElseThrow(() -> new RuntimeException("Invitation not found"));
        groupValidator.validateRespondToGroupInvitation(groupInvitation);

        if (status == InvitationStatus.ACCEPTED) {
            groupMembershipRepository.save(
                    GroupMembership.builder()
                            .user(groupInvitation.getUser())
                            .group(groupInvitation.getGroup())
                            .role(groupInvitation.getUserToBeInvitedRole())
                            .build()
            );
        }

        touchLastGroupEventDate(groupInvitation.getGroup());
        groupEventRepository.save(GroupEvent.builder()
                .group(groupInvitation.getGroup())
                .description(groupInvitation.getUser().getName() + " has been added to the group")
                .build());

        groupInvitation.setInvitationStatus(status);
        return groupInvitationRepository.save(groupInvitation);
        }

    @Override
    @Transactional(readOnly = true)
    public Set<GroupInvitation> findMyGroupInvitations() {
        return groupInvitationRepository.findIncomingByUserIdAndStatusWithFetch(
                effectiveCurrentUser.getUserId(), InvitationStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<GroupInvitation> findInvitationsSentByMe() {
        return groupInvitationRepository.findAllSentByInvitedByIdWithFetch(effectiveCurrentUser.getUserId());
    }


    @Override
    public Task createTask(Long groupId, Task task, Set<Long> assignedIds, Set<Long> reviewerIds, Set<MultipartFile> files) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        groupValidator.validateForCreateTask(groupId, assignedIds, reviewerIds);

        task.setGroup(groupRepository.getReferenceById(groupId));
        task.setCreatorIdSnapshot(effectiveCurrentUser.getUserId());
        task.setCreatorNameSnapshot(
                userRepository.findById(effectiveCurrentUser.getUserId()).orElseThrow(
                        ()-> new EntityNotFoundException("Can not find logged in user Id ;")).getName()
        );
        var savedTask =  taskRepository.save(task);

        savedTask.getTaskParticipants().add(
                TaskParticipant.builder()
                        .user(userRepository.getReferenceById(effectiveCurrentUser.getUserId()))
                        .task(savedTask)
                        .taskParticipantRole(TaskParticipantRole.CREATOR)
                        .build()
        );

        if (assignedIds != null && !assignedIds.isEmpty()){
            for(Long assignedId:assignedIds){

                var taskParticipant = TaskParticipant.builder()
                        .taskParticipantRole(TaskParticipantRole.ASSIGNEE)
                        .user(userRepository.getReferenceById(assignedId))
                        .task(savedTask)
                        .build();
                savedTask.getTaskParticipants().add(taskParticipant);
            }
        }

        if (reviewerIds != null && !reviewerIds.isEmpty()){
            for(Long reviewerId:reviewerIds){
                var taskParticipant = TaskParticipant.builder()
                        .taskParticipantRole(TaskParticipantRole.REVIEWER)
                        .user(userRepository.getReferenceById(reviewerId))
                        .task(savedTask)
                        .build();
                savedTask.getTaskParticipants().add(taskParticipant);
            }
        }

        if (files != null && !files.isEmpty()) {
            if (files.size() > 3) {
                throw new InvalidFieldValueException("Maximum 3 files allowed");
            }

            for (MultipartFile file : files){
                String url = blobStorageService.uploadTaskFile(file, savedTask.getId());
                var taskFile = TaskFile.builder()
                        .fileUrl(url)
                        .name(file.getOriginalFilename())
                        .task(savedTask)
                        .build();
                savedTask.getCreatorFiles().add(taskFile);
            }
        }
        taskRepository.save(savedTask);
//        emailClient.sendEmail("giovani1994a@gmail.com","testSub","the body message");
        //ToDO: email development to be added later. Message unified for all roles but one per id
        // included, email message example to be added:
        // New task created: “Fix invoice bug”
        // You were added to this task.
        // [Open task link]
        var thefetchedOne = taskRepository.findByIdWithFullFetchParticipantsAndFiles(savedTask.getId()).orElseThrow(
                () -> new TaskNotFoundException("Something went wrong with the create process of task." +
                "If the problem insist during creation conduct us and provide one of the tasks ID. Current taskId:"
                                                + savedTask.getId())
        );
        return thefetchedOne;
    }



    @Override
    @Transactional(readOnly = true)
    public Task getTask(Long groupId, Long taskId){
        authorizationService.requireAnyRoleIn(groupId);
        return taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId).orElseThrow(()-> new TaskNotFoundException(
                "Task with id " + taskId + "is not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Task> findMyTasks(Long groupId, Boolean reviewer, Boolean assigned, TaskState taskState) {
        return taskRepository.searchBy(groupId, effectiveCurrentUser.getUserId(), reviewer, assigned, taskState);
    }




    @Override
    public Task patchTask(Long groupId, Long taskId, Task task) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        groupValidator.validateForPatchTask(groupId, taskId, task);
        User curUser = userRepository.getReferenceById(effectiveCurrentUser.getUserId());
        var fetchedTask = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + "is not found"));
        if (task.getTitle()!= null)
            fetchedTask.setTitle(task.getTitle());
        if (task.getDescription()!= null)
            fetchedTask.setDescription(task.getDescription());
        if (task.getTaskState()!= null)
            fetchedTask.setTaskState(task.getTaskState());
        if (task.getDueDate() != null)
            fetchedTask.setDueDate(task.getDueDate());
        if (task.getReviewersDecision()!=null){
            fetchedTask.setReviewersDecision(task.getReviewersDecision());
            fetchedTask.setReviewedBy(curUser);
        }
        if (task.getReviewComment()!=null){
            fetchedTask.setReviewComment(task.getReviewComment());
        }
        fetchedTask.setLastEditBy(curUser);
        fetchedTask.setLastEditDate(Instant.now());
        return taskRepository.save(fetchedTask);
    }

    @Override
    public Task reviewTask(Long groupId, Long taskId, Task task) {
        var fetchedTask = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));

        groupValidator.validateReviewTask(fetchedTask, groupId, effectiveCurrentUser.getUserId());

        if (task.getReviewersDecision() != null) {
            fetchedTask.setReviewersDecision(task.getReviewersDecision());
            fetchedTask.setReviewedBy(userRepository.getReferenceById(effectiveCurrentUser.getUserId()));
            if (task.getReviewersDecision() == ReviewersDecision.APPROVE) {
                fetchedTask.setTaskState(TaskState.DONE);
            } else {
                fetchedTask.setTaskState(TaskState.IN_PROGRESS);
            }
        }
        if (task.getReviewComment() != null) {
            fetchedTask.setReviewComment(task.getReviewComment());
        }

        return taskRepository.save(fetchedTask);
    }



    @Override
    public Task addTaskParticipant(Long groupId, Long taskId, Long userId, TaskParticipantRole taskParticipantRole) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
        .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));

        if (taskParticipantRole == TaskParticipantRole.ASSIGNEE){
            groupValidator.validateAddAssigneeToTask(task, groupId, userId);
        }else if (taskParticipantRole == TaskParticipantRole.REVIEWER){
            groupValidator.validateAddReviewerToTask(task, groupId, userId);
        }else{
            throw new InvalidFieldValueException("Currently you may not other roles than assignee or reviewer");
        }

        task.getTaskParticipants().add(TaskParticipant.builder()
        .task(task)
        .user(userRepository.getReferenceById(userId))
        .taskParticipantRole(taskParticipantRole)
        .build());
        return taskRepository.save(task);
    }

    @Override
    public void removeTaskParticipant(Long groupId, Long taskId, Long taskParticipantId) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
        .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));
        groupValidator.validateRemoveTaskParticipant(task, groupId, taskParticipantId);

        task.getTaskParticipants().removeIf(tp ->
        tp.getId().equals(taskParticipantId));
    }


    @Override
    public Task addTaskFile(Long groupId, Long taskId, MultipartFile file) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));
        groupValidator.validateAddTaskFile(task, groupId, file);
        String url = blobStorageService.uploadTaskFile(file,taskId);
        task.getCreatorFiles().add(TaskFile.builder()
                .task(task)
                .name(file.getOriginalFilename())
                .fileUrl(url)
                .build());
        return taskRepository.save(task);

    }

    @Transactional(readOnly = true)
    public TaskFileDownload downloadTaskFile(Long groupId, Long taskId, Long fileId) {
        authorizationService.requireAnyRoleIn(groupId);

        var task = taskRepository.findByIdWithParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));

        groupValidator.validateDownloadTaskFile(task, groupId);

        TaskFile file = task.getCreatorFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new TaskFileNotFoundException("File not found"));
        byte[] data = blobStorageService.downloadTaskFile(file.getFileUrl());
        return new TaskFileDownload(data, file.getName());

    }





    @Override
    public void removeTaskFile(Long groupId, Long taskId, Long fileId) {
    authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));
        groupValidator.validateRemoveTaskFile(task, groupId, fileId);

        TaskFile file = task.getCreatorFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow();

        task.getCreatorFiles().remove(file);
        taskFileRepository.delete(file);
    }

        @Override
        public Task addAssigneeTaskFile(Long groupId, Long taskId, MultipartFile file) {
        authorizationService.requireAnyRoleIn(groupId);

        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
            .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));

        groupValidator.validateAddAssigneeTaskFile(task, groupId, file);

        String url = blobStorageService.uploadTaskFile(file, taskId);
        task.getAssigneeFiles().add(TaskAssigneeFile.builder()
            .task(task)
            .name(file.getOriginalFilename())
            .fileUrl(url)
            .build());
        return taskRepository.save(task);
        }

        @Override
        public void removeAssigneeTaskFile(Long groupId, Long taskId, Long fileId) {
        authorizationService.requireAnyRoleIn(groupId);

            var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
            .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));

        groupValidator.validateRemoveAssigneeTaskFile(task, groupId, fileId);

        TaskAssigneeFile file = task.getAssigneeFiles().stream()
            .filter(f -> f.getId().equals(fileId))
            .findFirst()
            .orElseThrow(() -> new TaskFileNotFoundException("File not found"));

        task.getAssigneeFiles().remove(file);
        taskAssigneeFileRepository.delete(file);
        }

        @Override
        @Transactional(readOnly = true)
        public TaskFileDownload downloadAssigneeTaskFile(Long groupId, Long taskId, Long fileId) {
        authorizationService.requireAnyRoleIn(groupId);

        var task = taskRepository.findByIdWithParticipantsAndFiles(taskId)
            .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));

        groupValidator.validateDownloadTaskFile(task, groupId);

        TaskAssigneeFile file = task.getAssigneeFiles().stream()
            .filter(f -> f.getId().equals(fileId))
            .findFirst()
            .orElseThrow(() -> new TaskFileNotFoundException("File not found"));
        byte[] data = blobStorageService.downloadTaskFile(file.getFileUrl());
        return new TaskFileDownload(data, file.getName());
        }

        @Override
        public Task markTaskToBeReviewed(Long groupId, Long taskId) {
        authorizationService.requireAnyRoleIn(groupId);

        var task = taskRepository.findByIdWithTaskParticipants(taskId)
            .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));

        groupValidator.validateAssigneeMarkTaskToBeReviewed(task, groupId);
        task.setTaskState(TaskState.TO_BE_REVIEWED);
        taskRepository.save(task);

        return taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
            .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));
        }

    @Override
    @Transactional
    public TaskComment addTaskComment(Long groupId, Long taskId, String comment) {
       //authorization checked anyway in the group validator in this case
        var task = taskRepository.findByIdWithParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));

        groupValidator.validateTaskComment(groupId,task,comment);

        TaskComment saved = taskCommentRepository.save(
                TaskComment.builder()
                .task(taskRepository.getReferenceById(taskId))
                .creator(userRepository.getReferenceById(effectiveCurrentUser.getUserId()))
                .comment(comment)
                .build()
        );

        long currentCount = taskCommentRepository.countByTask_Id(taskId);
        task.setCommentCount(currentCount);
        task.setLastCommentDate(Instant.now());
        taskRepository.save(task);

        return saved;
    }

    @Override
    public TaskComment patchTaskComment(Long groupId, Long taskId, Long commentId, String comment) {
        authorizationService.requireAnyRoleIn(groupId);
        var existing = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Task comment not found"));
        groupValidator.validateTaskPatchComment(groupId,existing,taskId,comment);
        existing.setComment(comment);
        return taskCommentRepository.save(existing);
    }

    @Override
    public void deleteTaskComment(Long groupId, Long taskId, Long commentId) {
        TaskComment existing = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Task comment not found"));
        Task task = existing.getTask();
        groupValidator.validateDeleteTask(groupId,existing, task, commentId);
        taskCommentRepository.delete(existing);
        long currentCount = taskCommentRepository.countByTask_Id(task.getId());
        task.setCommentCount(currentCount);
        taskRepository.save(task);
    }


}
