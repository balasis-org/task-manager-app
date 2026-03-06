package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.authorization.UnauthorizedException;
import io.github.balasis.taskmanager.context.base.utils.StringSanitizer;
import io.github.balasis.taskmanager.contracts.enums.BlobDefaultImageContainer;
import io.github.balasis.taskmanager.engine.core.dto.GroupRefreshDto;
import io.github.balasis.taskmanager.engine.core.dto.GroupWithPreviewDto;
import io.github.balasis.taskmanager.engine.core.dto.TaskPreviewDto;
import io.github.balasis.taskmanager.context.base.enumeration.InvitationStatus;
import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;
import io.github.balasis.taskmanager.context.base.enumeration.TaskParticipantRole;
import io.github.balasis.taskmanager.context.base.enumeration.ReviewersDecision;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.base.exception.authorization.NotAGroupMemberException;
import io.github.balasis.taskmanager.context.base.exception.business.BusinessRuleException;
import io.github.balasis.taskmanager.context.base.exception.business.InvalidMembershipRemovalException;
import io.github.balasis.taskmanager.context.base.exception.business.LimitExceededException;
import io.github.balasis.taskmanager.context.base.exception.ratelimit.DownloadBudgetExceededException;
import io.github.balasis.taskmanager.context.base.limits.PlanLimits;
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
import io.github.balasis.taskmanager.engine.infrastructure.email.TaskEmailTemplates;
import io.github.balasis.taskmanager.engine.infrastructure.redis.DownloadGuardService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.ImageChangeLimiterService;
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
    private final DeletedTaskRepository deletedTaskRepository;
    private final MaintenanceStatusRepository maintenanceStatusRepository;
    private final PlanLimits planLimits;
    private final DownloadGuardService downloadGuardService;
    private final ImageChangeLimiterService imageChangeLimiterService;
    private final org.springframework.core.env.Environment environment;

    // ── limit-resolution helpers ────────────────────────────────────────
    // 3-level fallback: task override → group override → plan default.
    // Overrides can only tighten (lower) the plan limit, never widen it.

    private User findGroupLeader(Long groupId) {
        return groupMembershipRepository
                .findByGroup_IdAndRole(groupId, Role.GROUP_LEADER)
                .orElseThrow(() -> new BusinessRuleException("Group has no leader"))
                .getUser();
    }

    private int resolveMaxCreatorFiles(Task task, Group group, SubscriptionPlan plan) {
        int planDefault = planLimits.maxCreatorFilesPerTask(plan);
        if (task.getMaxCreatorFiles() != null) return Math.min(task.getMaxCreatorFiles(), planDefault);
        if (group.getMaxCreatorFilesPerTask() != null) return Math.min(group.getMaxCreatorFilesPerTask(), planDefault);
        return planDefault;
    }

    private int resolveMaxAssigneeFiles(Task task, Group group, SubscriptionPlan plan) {
        int planDefault = planLimits.maxAssigneeFilesPerTask(plan);
        if (task.getMaxAssigneeFiles() != null) return Math.min(task.getMaxAssigneeFiles(), planDefault);
        if (group.getMaxAssigneeFilesPerTask() != null) return Math.min(group.getMaxAssigneeFilesPerTask(), planDefault);
        return planDefault;
    }

    private long resolveMaxFileSizeBytes(Task task, Group group, SubscriptionPlan plan) {
        long planDefault = planLimits.maxFileSizeBytes(plan);
        if (task.getMaxFileSizeBytes() != null) return Math.min(task.getMaxFileSizeBytes(), planDefault);
        if (group.getMaxFileSizeBytes() != null) return Math.min(group.getMaxFileSizeBytes(), planDefault);
        return planDefault;
    }

    /**
     * Charges the leader's storage budget atomically.
     * FREE tier has no budget tracking (returns immediately).
     * Throws if the paid-tier budget would be exceeded.
     */
    private void chargeStorageBudget(User leader, long sizeBytes) {
        if (!planLimits.isPaid(leader.getSubscriptionPlan())) return;
        long budget = planLimits.storageBudgetBytes(leader.getSubscriptionPlan());
        int updated = userRepository.addStorageUsage(leader.getId(), sizeBytes, budget);
        if (updated == 0) {
            throw new LimitExceededException("Storage budget exceeded. Delete some files or upgrade your plan.");
        }
    }

    /**
     * Refunds storage on file deletion. NULL / zero sizes (legacy files
     * uploaded before budget tracking) are silently skipped.
     */
    private void refundStorageBudget(User leader, Long fileSizeBytes) {
        if (fileSizeBytes == null || fileSizeBytes <= 0) return;
        if (!planLimits.isPaid(leader.getSubscriptionPlan())) return;
        userRepository.subtractStorageUsage(leader.getId(), fileSizeBytes);
    }

    // Charges the group owner's monthly download budget. Legacy files with
    // size 0 pass through for free (uploaded before budget tracking).
    private void chargeDownloadBudget(Long groupId, long sizeBytes) {
        if (sizeBytes <= 0) return;
        User owner = findGroupLeader(groupId);
        SubscriptionPlan ownerPlan = owner.getSubscriptionPlan();
        long budget = planLimits.downloadBudgetBytes(ownerPlan);
        int updated = userRepository.addDownloadUsage(owner.getId(), sizeBytes, budget);
        if (updated == 0) {
            throw new DownloadBudgetExceededException(
                    "Your group's download budget for this month has been reached.");
        }
    }

    // If the group's repeat download guard is enabled, delegates to Redis
    // to enforce the per-file daily cap. Best-effort — failures are swallowed.
    private void checkRepeatGuard(Group group, Long fileId) {
        if (!Boolean.TRUE.equals(group.getDailyDownloadCapEnabled())) return;
        long userId = effectiveCurrentUser.getUserId();
        downloadGuardService.checkRepeatDownload(userId, fileId);
    }

    @Override
    public Group create(Group group){
        var user = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(()->new UserNotFoundException("User not found"));

        long groupCount = groupMembershipRepository.countByUser_Id(user.getId());
        int maxGroups = planLimits.maxGroups(user.getSubscriptionPlan());
        if (groupCount >= maxGroups) {
            throw new LimitExceededException("Cannot create group. You have reached the max membership number (" + maxGroups + ")");
        }

        int maxCreations = planLimits.maxGroupCreationsPerWindow(user.getSubscriptionPlan());
        if (user.getTotalGroupsCreated() >= maxCreations) {
            String resetHint = maintenanceStatusRepository.findById(1L)
                    .map(MaintenanceStatus::getNextResetAt)
                    .filter(Objects::nonNull)
                    .map(next -> "You can create more groups after "
                            + DateTimeFormatter.ofPattern("MMM d 'at' HH:mm 'UTC'")
                                    .withZone(ZoneOffset.UTC).format(next) + ".")
                    .orElse("Please try again later.");
            throw new LimitExceededException(
                    "You've reached the limit of " + maxCreations
                            + " group creations. " + resetHint);
        }

        groupValidator.validate(group);
        group.setOwner(user);
        group.setDefaultImgUrl(defaultImageService.pickRandom(BlobContainerType.GROUP_IMAGES));
        touchGroupChange(group, true);
        Group savedGroup = groupRepository.save(group);
        groupMembershipRepository.save(
                GroupMembership.builder()
                        .user(user)
                        .group(savedGroup)
                        .role(Role.GROUP_LEADER)
                        .build()
        );

        user.setTotalGroupsCreated(user.getTotalGroupsCreated() + 1);
        userRepository.save(user);

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
        Group existingGroup = groupRepository.findByIdWithOwner(groupId)
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
            boolean requested = group.getAllowEmailNotification();
            if (requested && planLimits.emailQuotaPerMonth(
                    existingGroup.getOwner().getSubscriptionPlan()) <= 0) {
                requested = false;
            }
            existingGroup.setAllowEmailNotification(requested);
            touchLastGroupEventDate(existingGroup);
            var gEvAllowEmail =  GroupEvent.builder().group(existingGroup)
                    .description(  (requested
                    ? "Group email notifications are enabled"
                    : "Group email notifications are disabled"))
                    .build();
            groupEventRepository.save(gEvAllowEmail);
        }

        if (group.getDailyDownloadCapEnabled() != null) {
            existingGroup.setDailyDownloadCapEnabled(group.getDailyDownloadCapEnabled());
            touchLastGroupEventDate(existingGroup);
            var gEvCap = GroupEvent.builder().group(existingGroup)
                    .description(group.getDailyDownloadCapEnabled()
                            ? "Repeat download guard enabled"
                            : "Repeat download guard disabled")
                    .build();
            groupEventRepository.save(gEvCap);
        }

        if (group.getAllowAssigneeEmailNotification() != null) {
            boolean requested = group.getAllowAssigneeEmailNotification();
            if (requested && planLimits.emailQuotaPerMonth(
                    existingGroup.getOwner().getSubscriptionPlan()) <= 0) {
                requested = false;
            }
            existingGroup.setAllowAssigneeEmailNotification(requested);
            touchLastGroupEventDate(existingGroup);
            var gEvAssigneeEmail = GroupEvent.builder().group(existingGroup)
                    .description(requested
                            ? "Assignee-to-reviewer email notifications enabled"
                            : "Assignee-to-reviewer email notifications disabled")
                    .build();
            groupEventRepository.save(gEvAssigneeEmail);
        }

        touchGroupChange(existingGroup, true);

        return groupRepository.save(existingGroup);
    }

    @Override
    public void delete(Long groupId) {
        authorizationService.requireRoleIn(groupId,Set.of(Role.GROUP_LEADER));

        Group group = groupRepository.findById(groupId).orElse(null);
        if (group != null && group.getImgUrl() != null) {
            blobStorageService.deleteGroupImage(group.getImgUrl());
        }

        groupInvitationRepository.deleteAllByGroup_Id(groupId);
        deletedTaskRepository.deleteAllByGroup_Id(groupId);
        taskRepository.deleteAllByGroup_Id(groupId);
        groupMembershipRepository.deleteAllByGroup_Id(groupId);
        groupRepository.deleteById(groupId);
    }

    @Override
    public Group updateGroupImage(Long groupId, MultipartFile file) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER));

        Group group = groupRepository.findByIdWithOwner(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with id " + groupId + " not found"));

        User owner = group.getOwner();
        SubscriptionPlan ownerPlan = owner.getSubscriptionPlan();

        if (!planLimits.isPaid(ownerPlan)) {
            throw new BusinessRuleException(
                    "Custom group images require a paid plan. Choose a default image instead.");
        }

        imageChangeLimiterService.checkBurstLimit(effectiveCurrentUser.getUserId());

        int maxScans = planLimits.imageScansPerMonth(ownerPlan);
        int updated = userRepository.incrementImageScanUsage(owner.getId(), maxScans);
        if (updated == 0) {
            throw new LimitExceededException(
                    "Monthly image upload limit for this group has been reached.");
        }

        String oldBlobName = group.getImgUrl();

        String blobName = blobStorageService.uploadGroupImage(file, groupId);
        group.setImgUrl(blobName);
        touchGroupChange(group, true);

        if (oldBlobName != null) {
            blobStorageService.deleteGroupImage(oldBlobName);
        }

        return groupRepository.save(group);
    }

    @Override
    public Group pickDefaultGroupImage(Long groupId, String fileName) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER));

        Group group = groupRepository.findByIdWithOwner(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with id " + groupId + " not found"));

        String oldBlobName = group.getImgUrl();

        group.setDefaultImgUrl(fileName);
        group.setImgUrl(null);
        touchGroupChange(group, true);

        if (oldBlobName != null) {
            blobStorageService.deleteGroupImage(oldBlobName);
        }

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
    @Transactional
    public void deleteAllGroupEvents(Long groupId) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER));
        groupEventRepository.deleteAllByGroup_Id(groupId);
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
        Role currentUserRole = membershipOpt.isPresent() ? membershipOpt.get().getRole() : null;

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
                    .creatorName(task.getCreatorNameSnapshot())
                    .priority(task.getPriority())
                    .deletable(computeIsDeletable(task, currentUserRole, groupId, currentUserId))
                    .build();
            })
            .collect(Collectors.toSet());

        var groupImgUrlConverted = (group.getImgUrl()==null || group.getImgUrl().isBlank())
                ? null
                : BlobContainerType.GROUP_IMAGES.getContainerName() + "/" + group.getImgUrl();

        var defaultImgUrlConverted = ((group.getDefaultImgUrl() == null) || group.getDefaultImgUrl().isBlank())
                ? null
                : BlobDefaultImageContainer.GROUP_IMAGES.getContainerName()+"/"+group.getDefaultImgUrl();

        var ownerPlan = group.getOwner().getSubscriptionPlan();

        return GroupWithPreviewDto.builder()
            .id(group.getId())
            .name(group.getName())
            .description(group.getDescription())
            .defaultImgUrl(defaultImgUrlConverted)
            .imgUrl(groupImgUrlConverted)
            .ownerId(group.getOwner().getId())
            .ownerName(group.getOwner().getName())
            .announcement(group.getAnnouncement())
            .allowEmailNotification(group.getAllowEmailNotification())
            .createdAt(group.getCreatedAt())
            .ownerPlan(ownerPlan.name())
            .downloadBudgetBytes(planLimits.downloadBudgetBytes(ownerPlan))
            .usedDownloadBytesMonth(group.getOwner().getUsedDownloadBytesMonth())
            .storageBudgetBytes(planLimits.storageBudgetBytes(ownerPlan))
            .usedStorageBytes(group.getOwner().getUsedStorageBytes())
            .maxCreatorFiles(planLimits.maxCreatorFilesPerTask(ownerPlan))
            .maxAssigneeFiles(planLimits.maxAssigneeFilesPerTask(ownerPlan))
            .maxFileSizeBytes(planLimits.maxFileSizeBytes(ownerPlan))
            .maxTasks(planLimits.maxTasksPerGroup(ownerPlan))
            .maxMembers(planLimits.maxMembersPerGroup(ownerPlan))
            .dailyDownloadCapEnabled(group.getDailyDownloadCapEnabled())
            .allowAssigneeEmailNotification(group.getAllowAssigneeEmailNotification())
            .usedEmailsMonth(group.getOwner().getUsedEmailsMonth())
            .emailQuotaPerMonth(planLimits.emailQuotaPerMonth(ownerPlan))
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
        Role currentUserRole = membershipOpt.isPresent() ? membershipOpt.get().getRole() : null;

        Long effectiveCreatorId = Boolean.TRUE.equals(creatorIsMe) ? currentUserId : creatorId;
        Long effectiveReviewerId = Boolean.TRUE.equals(reviewerIsMe) ? currentUserId : reviewerId;
        Long effectiveAssigneeId = Boolean.TRUE.equals(assigneeIsMe) ? currentUserId : assigneeId;

        boolean hasFilters =
                effectiveCreatorId != null ||
                effectiveReviewerId != null ||
                effectiveAssigneeId != null ||
                dueDateBefore != null;

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
                        .creatorName(task.getCreatorNameSnapshot())
                        .priority(task.getPriority())
                        .deletable(computeIsDeletable(task, currentUserRole, groupId, currentUserId))
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
                        .creatorName(task.getCreatorNameSnapshot())
                        .priority(task.getPriority())
                        .deletable(computeIsDeletable(task, currentUserRole, groupId, currentUserId))
                        .build();
                })
                .collect(Collectors.toSet());
    }

    @Override
    public Page<TaskComment> findAllTaskComments(Long groupId, Long taskId ,Pageable pageable){
        authorizationService.requireAnyRoleIn(groupId);

        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));
        if (!task.getGroup().getId().equals(groupId))
            throw new TaskNotFoundException("Task does not belong to this group");

        var taskParticipants = taskParticipantRepository.findAllByTask_idAndUser_id(taskId, effectiveCurrentUser.getUserId());
        if (!taskParticipants.isEmpty()) {
            Instant now = Instant.now();
            for (var tp : taskParticipants) {
                tp.setLastSeenTaskComments(now);
            }
            taskParticipantRepository.saveAll(taskParticipants);
        }
        return taskCommentRepository.findAllByTask_id(taskId,pageable);
    }

    @Override
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
        touchGroupChange(gr, false);
        touchMemberChange(gr);
        taskCommentRepository.detachCreatorFromGroupComments(targetsId, groupId, targetsName);
        taskParticipantRepository.deleteByUserIdAndGroupId(targetsId, groupId);
        groupMembershipRepository.deleteByGroupIdAndUserId(groupId, targetsId);
    }

    @Override
    public GroupMembership changeGroupMembershipRole(Long groupId, Long groupMembershipId, Role newRole) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER));

        var targetOpt = groupMembershipRepository.findByIdWithUser(groupMembershipId);
        if (targetOpt.isEmpty()) {
            throw new GroupMembershipNotFoundException("Target membership not found");
        }
        GroupMembership target = targetOpt.get();
        groupValidator.validateChangeGroupMembershipRole(groupId, target.getUser().getId(), newRole);

        if (newRole != Role.TASK_MANAGER && newRole != Role.REVIEWER) {
            taskParticipantRepository.deleteReviewersByUserIdAndGroupId(target.getUser().getId(), groupId);
        }
        target.setRole(newRole);
        touchGroupChange(groupRepository.getReferenceById(groupId), false);
        touchMemberChange(groupRepository.getReferenceById(groupId));
        GroupMembership saved = groupMembershipRepository.save(target);
        return saved;
    }

    @Override
    public void createGroupInvitation(Long groupId, String inviteCode, Role userToBeInvitedRole, String comment, boolean sendEmail){
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));

        var group = groupRepository.findById(groupId).orElseThrow();

        String code = (inviteCode == null) ? "" : inviteCode.trim().toUpperCase();

        var targetUserOpt = userRepository.findByInviteCode(code);
        if (targetUserOpt.isEmpty()) {
            logger.debug("Invite attempt with unknown code in group {}", groupId);
            return;
        }
        var targetUser = targetUserOpt.get();

        if (groupMembershipRepository.existsByGroupIdAndUserId(groupId, targetUser.getId())) {
            logger.debug("Invite attempt for user {} who is already member of group {}", targetUser.getId(), groupId);
            return;
        }

        if (groupInvitationRepository.existsByUser_IdAndGroup_IdAndInvitationStatus(
                targetUser.getId(), groupId, InvitationStatus.PENDING)) {
            logger.debug("Duplicate invite attempt for user {} in group {}", targetUser.getId(), groupId);
            return;
        }

        // member cap — reject invitation early if group is full
        long memberCount = groupMembershipRepository.countByGroup_Id(groupId);
        int maxMembers = planLimits.maxMembersPerGroup(group.getOwner().getSubscriptionPlan());
        if (memberCount >= maxMembers) {
            throw new LimitExceededException(
                    "This group has reached its member limit (" + maxMembers + ")");
        }

        var inviter = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Inviter not found"));

        var role = (userToBeInvitedRole == null) ? Role.MEMBER : userToBeInvitedRole;

        var groupInvitation = GroupInvitation.builder()
                .user(targetUser)
                .invitationStatus(InvitationStatus.PENDING)
                .invitedBy(inviter)
                .group(group)
                .userToBeInvitedRole(role)
                .comment((comment == null || comment.isBlank()) ? null : comment.trim())
                .build();

        groupValidator.validateInvitationRole(groupInvitation);

        GroupInvitation saved = groupInvitationRepository.save(groupInvitation);

        targetUser.setLastInviteReceivedAt(Instant.now());
        userRepository.save(targetUser);

        boolean groupAllows = sendEmail;
        boolean userAllows = Boolean.TRUE.equals(targetUser.getAllowEmailNotification());

        if (groupAllows && userAllows && targetUser.getEmail() != null && !targetUser.getEmail().isBlank()) {
            // email notifications are an Organizer+ feature — check owner quota
            User owner = group.getOwner();
            int quota = planLimits.emailQuotaPerMonth(owner.getSubscriptionPlan());
            if (quota <= 0) return; // tier has no email quota — skip silently
            int updated = userRepository.incrementEmailUsage(owner.getId(), quota);
            if (updated == 0) return; // monthly quota exhausted — skip silently

            EmailClient emailClient = emailClientProvider.getIfAvailable();
            if (emailClient != null) {
                String subject = TaskEmailTemplates.inviteSubject(group.getName());
                String body = TaskEmailTemplates.inviteBody(
                        group.getName(), inviter.getName(),
                        (saved.getComment() != null && !saved.getComment().isBlank()) ? saved.getComment() : null);
                final String emailTo = targetUser.getEmail();
                CompletableFuture.runAsync(() -> emailClient.sendEmail(emailTo, subject, body));
            }
        }
    }

        @Override
        public GroupInvitation respondToInvitation(Long groupInvitationId, InvitationStatus status) {
        var groupInvitation = groupInvitationRepository.findByIdWithGroup(groupInvitationId)
            .orElseThrow(() -> new io.github.balasis.taskmanager.context.base.exception.notfound.EntityNotFoundException("Invitation not found"));
        groupValidator.validateRespondToGroupInvitation(groupInvitation);

        if (status == InvitationStatus.ACCEPTED) {
            var invitedUser = groupInvitation.getUser();
            long groupCount = groupMembershipRepository.countByUser_Id(invitedUser.getId());
            int maxGroups = planLimits.maxGroups(invitedUser.getSubscriptionPlan());
            if (groupCount >= maxGroups) {
                throw new LimitExceededException("You can be a member of at most " + maxGroups + " groups");
            }

            // member cap — double-check at accept time in case group filled since invite was sent
            var group = groupInvitation.getGroup();
            long memberCount = groupMembershipRepository.countByGroup_Id(group.getId());
            int maxMembers = planLimits.maxMembersPerGroup(group.getOwner().getSubscriptionPlan());
            if (memberCount >= maxMembers) {
                throw new LimitExceededException(
                        "This group has reached its member limit (" + maxMembers + ")");
            }

            groupMembershipRepository.save(
                    GroupMembership.builder()
                            .user(groupInvitation.getUser())
                            .group(groupInvitation.getGroup())
                            .role(groupInvitation.getUserToBeInvitedRole())
                            .build()
            );

            touchLastGroupEventDate(groupInvitation.getGroup());
            touchGroupChange(groupInvitation.getGroup(), false);
            touchMemberChange(groupInvitation.getGroup());
            groupEventRepository.save(GroupEvent.builder()
                    .group(groupInvitation.getGroup())
                    .description(groupInvitation.getUser().getName() + " has been added to the group")
                    .build());
        }

        groupInvitation.setInvitationStatus(status);

        var respondingUser = userRepository.findById(effectiveCurrentUser.getUserId()).orElse(null);
        if (respondingUser != null) {
            respondingUser.setLastSeenInvites(Instant.now());
            userRepository.save(respondingUser);
        }

        return groupInvitationRepository.save(groupInvitation);
        }

    @Override
    public Set<GroupInvitation> findMyGroupInvitations() {
        Long userId = effectiveCurrentUser.getUserId();
        var me = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        me.setLastSeenInvites(Instant.now());
        userRepository.save(me);

        return groupInvitationRepository.findIncomingByUserIdAndStatusWithFetch(
                userId, InvitationStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<GroupInvitation> findInvitationsSentByMe() {
        return groupInvitationRepository.findAllSentByInvitedByIdAndStatusWithFetch(
                effectiveCurrentUser.getUserId(),
                InvitationStatus.PENDING
        );
    }

    @Override
    public Task createTask(Long groupId, Task task, Set<Long> assignedIds, Set<Long> reviewerIds, Set<MultipartFile> files) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        groupValidator.validateForCreateTask(groupId, assignedIds, reviewerIds);
        long taskCount = taskRepository.countByGroup_Id(groupId);
        var currentUser = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Logged in user not found"));
        int maxTasks = planLimits.maxTasksPerGroup(currentUser.getSubscriptionPlan());
        if (taskCount >= maxTasks) {
            throw new LimitExceededException("A group can have at most " + maxTasks + " tasks");
        }

        if (taskRepository.existsByTitleAndGroup_Id(task.getTitle(), groupId)) {
            throw new BusinessRuleException("A task with this title already exists in this group");
        }

        task.setGroup(groupRepository.getReferenceById(groupId));
        task.setCreatorIdSnapshot(effectiveCurrentUser.getUserId());
        task.setCreatorNameSnapshot(currentUser.getName());
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
            User leader = findGroupLeader(groupId);
            SubscriptionPlan leaderPlan = leader.getSubscriptionPlan();
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new GroupNotFoundException("Group not found"));

            int maxCreator = resolveMaxCreatorFiles(savedTask, group, leaderPlan);
            long maxFileSize = resolveMaxFileSizeBytes(savedTask, group, leaderPlan);

            if (files.size() > maxCreator) {
                throw new InvalidFieldValueException("Maximum " + maxCreator + " files allowed");
            }

            long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
            chargeStorageBudget(leader, totalSize);

            for (MultipartFile file : files){
                String url = blobStorageService.uploadTaskFile(file, savedTask.getId(), maxFileSize);
                var taskFile = TaskFile.builder()
                        .fileUrl(url)
                        .name(StringSanitizer.sanitizeFilename(file.getOriginalFilename()))
                        .task(savedTask)
                        .fileSize(file.getSize())
                        .build();
                savedTask.getCreatorFiles().add(taskFile);
            }
        }
        touchTaskChange(savedTask, true, true, false);
        taskRepository.save(savedTask);

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

        Long currentUserId = effectiveCurrentUser.getUserId();
        var membershipOpt = groupMembershipRepository.findByUser_IdAndGroup_Id(currentUserId, groupId);
        boolean isLeaderOrManager = membershipOpt != null && (
                membershipOpt.getRole() == Role.GROUP_LEADER ||
                membershipOpt.getRole() == Role.TASK_MANAGER
        );

        Task task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException(
                        "Task with id " + taskId + " is not found"));

        if (!isLeaderOrManager) {
            boolean isParticipant = task.getTaskParticipants().stream()
                    .anyMatch(tp -> tp.getUser().getId().equals(currentUserId));
            if (!isParticipant) {
                throw new TaskNotFoundException("Task with id " + taskId + " is not accessible");
            }
        }

        return task;
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

        if (task.getTitle() != null && taskRepository.existsByTitleAndGroup_IdAndIdNot(task.getTitle(), groupId, taskId)) {
            throw new BusinessRuleException("A task with this title already exists in this group");
        }

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
        touchTaskChange(fetchedTask, true, false, false);
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
        touchTaskChange(fetchedTask, true, false, false);

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
        .user(userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found")))
        .taskParticipantRole(taskParticipantRole)
        .build());
        touchTaskChange(task, false, true, false);
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
        touchTaskChange(task, false, true, false);
    }

    @Override
    public void notifyTaskParticipant(Long groupId, Long taskId, Long userId, String customNote) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));

        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));
        if (!task.getGroup().getId().equals(groupId))
            throw new TaskNotFoundException("Task does not belong to this group");

        var targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (targetUser.getEmail() == null || targetUser.getEmail().isBlank()) {
            throw new LimitExceededException("User has no email address");
        }
        if (!Boolean.TRUE.equals(targetUser.getAllowEmailNotification())) {
            throw new LimitExceededException("User has disabled email notifications");
        }

        var group = groupRepository.findById(groupId).orElseThrow();
        User owner = group.getOwner();
        int quota = planLimits.emailQuotaPerMonth(owner.getSubscriptionPlan());
        if (quota <= 0) throw new LimitExceededException("Your plan does not include email notifications");
        int updated = userRepository.incrementEmailUsage(owner.getId(), quota);
        if (updated == 0) throw new LimitExceededException("Monthly email quota exhausted");

        var caller = userRepository.findById(effectiveCurrentUser.getUserId()).orElseThrow();

        String safeNote = (customNote != null) ? customNote.strip() : "";
        if (safeNote.length() > 300) safeNote = safeNote.substring(0, 300);

        String appUrl = environment.getProperty("app.public-url", "http://localhost:5173");

        EmailClient emailClient = emailClientProvider.getIfAvailable();
        if (emailClient != null) {
            String subject = TaskEmailTemplates.notifySubject(task.getTitle());
            String body = TaskEmailTemplates.notifyBody(
                    caller.getName(), task.getTitle(), group.getName(),
                    groupId, taskId, safeNote, appUrl);
            final String emailTo = targetUser.getEmail();
            CompletableFuture.runAsync(() -> emailClient.sendEmail(emailTo, subject, body));
        }
    }

    @Override
    public void notifyTaskParticipants(Long groupId, Long taskId, Set<Long> userIds, String customNote) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));

        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));
        if (!task.getGroup().getId().equals(groupId))
            throw new TaskNotFoundException("Task does not belong to this group");

        var group = groupRepository.findById(groupId).orElseThrow();
        User owner = group.getOwner();
        int quota = planLimits.emailQuotaPerMonth(owner.getSubscriptionPlan());
        if (quota <= 0) throw new LimitExceededException("Your plan does not include email notifications");

        var caller = userRepository.findById(effectiveCurrentUser.getUserId()).orElseThrow();

        String safeNote = (customNote != null) ? customNote.strip() : "";
        if (safeNote.length() > 300) safeNote = safeNote.substring(0, 300);

        String appUrl = environment.getProperty("app.public-url", "http://localhost:5173");

        List<User> targets = userRepository.findAllById(userIds).stream()
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .filter(u -> Boolean.TRUE.equals(u.getAllowEmailNotification()))
                .toList();

        if (targets.isEmpty()) throw new LimitExceededException("No reachable recipients");

        // charge one email unit per recipient
        for (int i = 0; i < targets.size(); i++) {
            int charged = userRepository.incrementEmailUsage(owner.getId(), quota);
            if (charged == 0) throw new LimitExceededException(
                    "Monthly email quota exhausted after " + i + " of " + targets.size() + " emails");
        }

        EmailClient emailClient = emailClientProvider.getIfAvailable();
        if (emailClient != null) {
            String subject = TaskEmailTemplates.notifySubject(task.getTitle());
            String body = TaskEmailTemplates.notifyBody(
                    caller.getName(), task.getTitle(), group.getName(),
                    groupId, taskId, safeNote, appUrl);
            for (User target : targets) {
                final String emailTo = target.getEmail();
                CompletableFuture.runAsync(() -> emailClient.sendEmail(emailTo, subject, body));
            }
        }
    }

    @Override
    public Task addTaskFile(Long groupId, Long taskId, MultipartFile file) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));

        User leader = findGroupLeader(groupId);
        SubscriptionPlan leaderPlan = leader.getSubscriptionPlan();
        Group group = task.getGroup();
        int maxCreator = resolveMaxCreatorFiles(task, group, leaderPlan);
        long maxFileSize = resolveMaxFileSizeBytes(task, group, leaderPlan);

        groupValidator.validateAddTaskFile(task, groupId, file, maxCreator);

        chargeStorageBudget(leader, file.getSize());

        String url = blobStorageService.uploadTaskFile(file, taskId, maxFileSize);
        task.getCreatorFiles().add(TaskFile.builder()
                .task(task)
                .name(StringSanitizer.sanitizeFilename(file.getOriginalFilename()))
                .fileUrl(url)
                .fileSize(file.getSize())
                .build());
        touchTaskChange(task, false, false, false);
        return taskRepository.save(task);

    }

    @Transactional // read-write: charges the owner's monthly download budget
    public TaskFileDownload downloadTaskFile(Long groupId, Long taskId, Long fileId) {
        authorizationService.requireAnyRoleIn(groupId);

        var task = taskRepository.findByIdWithParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));

        groupValidator.validateDownloadTaskFile(task, groupId);

        TaskFile file = task.getCreatorFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new TaskFileNotFoundException("File not found"));

        checkRepeatGuard(task.getGroup(), fileId);
        chargeDownloadBudget(groupId, file.getFileSize());

        var download = blobStorageService.downloadTaskFile(file.getFileUrl());
        return new TaskFileDownload(download.inputStream(), file.getName(), download.size());

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

        User leader = findGroupLeader(groupId);
        refundStorageBudget(leader, file.getFileSize());

        blobStorageService.deleteTaskFile(file.getFileUrl());
        task.getCreatorFiles().remove(file);
        taskFileRepository.delete(file);
        touchTaskChange(task, false, false, false);
    }

        @Override
        public Task addAssigneeTaskFile(Long groupId, Long taskId, MultipartFile file) {
        authorizationService.requireAnyRoleIn(groupId);

        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
            .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));

        User leader = findGroupLeader(groupId);
        SubscriptionPlan leaderPlan = leader.getSubscriptionPlan();
        Group group = task.getGroup();
        int maxAssignee = resolveMaxAssigneeFiles(task, group, leaderPlan);
        long maxFileSize = resolveMaxFileSizeBytes(task, group, leaderPlan);

        groupValidator.validateAddAssigneeTaskFile(task, groupId, file, maxAssignee);

        chargeStorageBudget(leader, file.getSize());

        String url = blobStorageService.uploadTaskAssigneeFile(file, taskId, maxFileSize);
        task.getAssigneeFiles().add(TaskAssigneeFile.builder()
            .task(task)
            .name(StringSanitizer.sanitizeFilename(file.getOriginalFilename()))
            .fileUrl(url)
            .fileSize(file.getSize())
            .build());
        touchTaskChange(task, false, false, false);
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

        User leader = findGroupLeader(groupId);
        refundStorageBudget(leader, file.getFileSize());

        blobStorageService.deleteTaskAssigneeFile(file.getFileUrl());
        task.getAssigneeFiles().remove(file);
        taskAssigneeFileRepository.delete(file);
        touchTaskChange(task, false, false, false);
        }

        @Override
        @Transactional // read-write: charges the owner's monthly download budget
        public TaskFileDownload downloadAssigneeTaskFile(Long groupId, Long taskId, Long fileId) {
        authorizationService.requireAnyRoleIn(groupId);

        var task = taskRepository.findByIdWithParticipantsAndFiles(taskId)
            .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));

        groupValidator.validateDownloadTaskFile(task, groupId);

        TaskAssigneeFile file = task.getAssigneeFiles().stream()
            .filter(f -> f.getId().equals(fileId))
            .findFirst()
            .orElseThrow(() -> new TaskFileNotFoundException("File not found"));

        checkRepeatGuard(task.getGroup(), fileId);
        chargeDownloadBudget(groupId, file.getFileSize());

        var download = blobStorageService.downloadTaskAssigneeFile(file.getFileUrl());
        return new TaskFileDownload(download.inputStream(), file.getName(), download.size());
        }

        @Override
        public Task markTaskToBeReviewed(Long groupId, Long taskId) {
        authorizationService.requireAnyRoleIn(groupId);

        var task = taskRepository.findByIdWithTaskParticipants(taskId)
            .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));

        groupValidator.validateAssigneeMarkTaskToBeReviewed(task, groupId);
        task.setTaskState(TaskState.TO_BE_REVIEWED);
        touchTaskChange(task, true, false, false);
        taskRepository.save(task);

        return taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
            .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));
        }

    @Override
    public TaskComment addTaskComment(Long groupId, Long taskId, String comment) {

        var task = taskRepository.findByIdWithParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));

        groupValidator.validateTaskComment(groupId,task,comment);

        var creator = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Logged in user not found"));

        TaskComment saved = taskCommentRepository.save(
                TaskComment.builder()
                .task(taskRepository.getReferenceById(taskId))
                .creator(creator)
                .creatorNameSnapshot(creator.getName())
                .comment(comment)
                .build()
        );

        long currentCount = taskCommentRepository.countByTask_Id(taskId);
        task.setCommentCount(currentCount);
        task.setLastCommentDate(Instant.now());
        touchTaskChange(task, false, false, true);
        taskRepository.save(task);

        return saved;
    }

    @Override
    public TaskComment patchTaskComment(Long groupId, Long taskId, Long commentId, String comment) {
        authorizationService.requireAnyRoleIn(groupId);
        var existing = taskCommentRepository.findWithCreatorById(commentId)
                .orElseThrow(() -> new RuntimeException("Task comment not found"));
        groupValidator.validateTaskPatchComment(groupId,existing,taskId,comment);
        existing.setComment(comment);
        touchTaskChange(existing.getTask(), false, false, true);
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
        touchTaskChange(task, false, false, true);
        taskRepository.save(task);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupRefreshDto refreshGroup(Long groupId, Instant lastSeen) {
        authorizationService.requireAnyRoleIn(groupId);
        Instant serverNow = Instant.now();

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with id " + groupId + " not found"));

        boolean groupChanged = group.getLastChangeInGroup() != null && group.getLastChangeInGroup().isAfter(lastSeen);
        boolean groupNoJoinsChanged = group.getLastChangeInGroupNoJoins() != null && group.getLastChangeInGroupNoJoins().isAfter(lastSeen);
        boolean tasksDeleted = group.getLastDeleteTaskDate() != null && group.getLastDeleteTaskDate().isAfter(lastSeen);
        boolean membersChanged = group.getLastMemberChangeDate() != null && group.getLastMemberChangeDate().isAfter(lastSeen);

        // always include plan-derived limits so the frontend stays up-to-date
        var ownerPlan = group.getOwner().getSubscriptionPlan();

        if (!groupChanged && !tasksDeleted) {
            return GroupRefreshDto.builder()
                    .serverNow(serverNow)
                    .changed(false)
                    .membersChanged(membersChanged)
                    .ownerPlan(ownerPlan.name())
                    .downloadBudgetBytes(planLimits.downloadBudgetBytes(ownerPlan))
                    .usedDownloadBytesMonth(group.getOwner().getUsedDownloadBytesMonth())
                    .storageBudgetBytes(planLimits.storageBudgetBytes(ownerPlan))
                    .usedStorageBytes(group.getOwner().getUsedStorageBytes())
                    .maxCreatorFiles(planLimits.maxCreatorFilesPerTask(ownerPlan))
                    .maxAssigneeFiles(planLimits.maxAssigneeFilesPerTask(ownerPlan))
                    .maxFileSizeBytes(planLimits.maxFileSizeBytes(ownerPlan))
                    .maxTasks(planLimits.maxTasksPerGroup(ownerPlan))
                    .maxMembers(planLimits.maxMembersPerGroup(ownerPlan))
                    .dailyDownloadCapEnabled(group.getDailyDownloadCapEnabled())
                    .allowAssigneeEmailNotification(group.getAllowAssigneeEmailNotification())
                    .build();
        }

        GroupRefreshDto.GroupRefreshDtoBuilder builder = GroupRefreshDto.builder()
                .serverNow(serverNow)
                .changed(true)
                .membersChanged(membersChanged)
                .ownerPlan(ownerPlan.name())
                .downloadBudgetBytes(planLimits.downloadBudgetBytes(ownerPlan))
                .usedDownloadBytesMonth(group.getOwner().getUsedDownloadBytesMonth())
                .storageBudgetBytes(planLimits.storageBudgetBytes(ownerPlan))
                .usedStorageBytes(group.getOwner().getUsedStorageBytes())
                .maxCreatorFiles(planLimits.maxCreatorFilesPerTask(ownerPlan))
                .maxAssigneeFiles(planLimits.maxAssigneeFilesPerTask(ownerPlan))
                .maxFileSizeBytes(planLimits.maxFileSizeBytes(ownerPlan))
                .maxTasks(planLimits.maxTasksPerGroup(ownerPlan))
                .maxMembers(planLimits.maxMembersPerGroup(ownerPlan))
                .dailyDownloadCapEnabled(group.getDailyDownloadCapEnabled())
                .allowAssigneeEmailNotification(group.getAllowAssigneeEmailNotification());

        if (groupNoJoinsChanged) {
            var groupImgUrlConverted = (group.getImgUrl() == null || group.getImgUrl().isBlank())
                    ? null
                    : BlobContainerType.GROUP_IMAGES.getContainerName() + "/" + group.getImgUrl();
            var defaultImgUrlConverted = (group.getDefaultImgUrl() == null || group.getDefaultImgUrl().isBlank())
                    ? null
                    : BlobDefaultImageContainer.GROUP_IMAGES.getContainerName() + "/" + group.getDefaultImgUrl();

            builder.name(group.getName())
                    .description(group.getDescription())
                    .announcement(group.getAnnouncement())
                    .defaultImgUrl(defaultImgUrlConverted)
                    .imgUrl(groupImgUrlConverted)
                    .allowEmailNotification(group.getAllowEmailNotification())
                    .allowAssigneeEmailNotification(group.getAllowAssigneeEmailNotification())
                    .lastGroupEventDate(group.getLastGroupEventDate());
        }

        Long currentUserId = effectiveCurrentUser.getUserId();
        var membershipOpt = groupMembershipRepository.findByGroupIdAndUserId(groupId, currentUserId);
        boolean isLeaderOrManager = membershipOpt.isPresent() && (
                membershipOpt.get().getRole() == Role.GROUP_LEADER ||
                        membershipOpt.get().getRole() == Role.TASK_MANAGER
        );
        Role currentUserRole = membershipOpt.isPresent() ? membershipOpt.get().getRole() : null;

        Set<Task> changedTasks = taskRepository.findChangedSince(groupId, lastSeen);
        Set<TaskPreviewDto> taskPreviews = changedTasks.stream()
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
                            .creatorName(task.getCreatorNameSnapshot())
                            .priority(task.getPriority())
                            .deletable(computeIsDeletable(task, currentUserRole, groupId, currentUserId))
                            .build();
                })
                .collect(Collectors.toSet());
        builder.changedTasks(taskPreviews);

        if (tasksDeleted) {
            Instant cutoff = lastSeen.minusSeconds(3);
            builder.deletedTaskIds(
                    deletedTaskRepository.findDeletedTaskIdsByGroupIdAndDeletedAtAfter(groupId, cutoff)
            );
        }

        return builder.build();
    }

    @Override
    public void deleteTask(Long groupId, Long taskId) {
        authorizationService.requireRoleIn(groupId,Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        var curUser = userRepository.findById(effectiveCurrentUser.getUserId()).orElseThrow();
        var taskToBeDeleted = taskRepository.findById(taskId).orElseThrow(
                ()-> new EntityNotFoundException("Task to be deleted doesnt exist"));
        var groupOfTask = groupRepository.findById(groupId).orElseThrow(
                ()-> new EntityNotFoundException("Group of task to be deleted doesnt exist"));
        if ( !(taskToBeDeleted.getGroup().getId() .equals(groupOfTask.getId())) ){
            throw new UnauthorizedException("Task doesn't belong to the group given");
        }

        var currentMembershipForDelete = groupMembershipRepository.findByGroupIdAndUserId(groupId, effectiveCurrentUser.getUserId());
        if (currentMembershipForDelete.isPresent() && currentMembershipForDelete.get().getRole() == Role.TASK_MANAGER) {
            Long creatorId = taskToBeDeleted.getCreatorIdSnapshot();
            if (creatorId != null && !creatorId.equals(effectiveCurrentUser.getUserId())) {
                var creatorMembership = groupMembershipRepository.findByGroupIdAndUserId(groupId, creatorId);
                boolean creatorIsProtected = creatorMembership.isPresent() &&
                    (creatorMembership.get().getRole() == Role.GROUP_LEADER ||
                     creatorMembership.get().getRole() == Role.TASK_MANAGER);
                if (creatorIsProtected) {
                    throw new UnauthorizedException("Task manager cannot delete a task created by a group leader or task manager.");
                }
            }
        }
        Instant now = Instant.now();
        var delTask = DeletedTask.builder()
            .deletedTaskId(taskId)
            .group(groupOfTask)
            .deletedAt(now)
            .build();
        deletedTaskRepository.save(delTask);
        groupOfTask.setLastDeleteTaskDate(now);
        taskRepository.deleteById(taskId);
        groupEventRepository.save(GroupEvent.builder()
                .group(groupOfTask)
                .description("Task '" + taskToBeDeleted.getTitle() + "' has been deleted by "
                + curUser.getName()).build());
        groupOfTask.setLastGroupEventDate(now);
        groupOfTask.setLastChangeInGroup(now);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findAccessibleTaskIds(Long groupId) {
        authorizationService.requireAnyRoleIn(groupId);
        Long currentUserId = effectiveCurrentUser.getUserId();

        var membershipOpt = groupMembershipRepository.findByUser_IdAndGroup_Id(currentUserId, groupId);
        boolean isLeaderOrManager = membershipOpt!=null && (
                membershipOpt.getRole() == Role.GROUP_LEADER ||
                membershipOpt.getRole() == Role.TASK_MANAGER
        );

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with id " + groupId + " not found"));

        return group.getTasks().stream()
                .filter(task -> isLeaderOrManager || task.getTaskParticipants().stream()
                        .anyMatch(tp -> tp.getUser().getId().equals(currentUserId)))
                .map(Task::getId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findFilteredTaskIds(
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
    ) {

        var membership = authorizationService.requireAnyRoleInAndGet(groupId);
        Long currentUserId = effectiveCurrentUser.getUserId();

        boolean isLeaderOrManager =
                membership.getRole() == Role.GROUP_LEADER ||
                membership.getRole() == Role.TASK_MANAGER;

        Long effectiveCreatorId  = Boolean.TRUE.equals(creatorIsMe)  ? currentUserId : creatorId;
        Long effectiveReviewerId = Boolean.TRUE.equals(reviewerIsMe) ? currentUserId : reviewerId;
        Long effectiveAssigneeId = Boolean.TRUE.equals(assigneeIsMe) ? currentUserId : assigneeId;

        Long participantUserId = isLeaderOrManager ? null : currentUserId;

        return taskRepository.filterTaskIds(
                groupId,
                effectiveCreatorId,
                effectiveReviewerId,
                effectiveAssigneeId,
                participantUserId,
                dueDateBefore,
                priorityMin,
                priorityMax,
                taskState,
                hasFiles
        );
    }

    private Instant touchLastGroupEventDate(Group group) {
        Instant now = Instant.now();
        group.setLastGroupEventDate(now);
        return now;
    }

    private void touchGroupChange(Group group, boolean noJoins) {
        Instant now = Instant.now();
        group.setLastChangeInGroup(now);
        if (noJoins) {
            group.setLastChangeInGroupNoJoins(now);
        }
    }

    private void touchMemberChange(Group group) {
        group.setLastMemberChangeDate(Instant.now());
    }

    private void touchTaskChange(Task task, boolean noJoins, boolean participants, boolean comments) {
        Instant now = Instant.now();
        task.setLastChangeDate(now);
        if (noJoins) task.setLastChangeDateNoJoins(now);
        if (participants) task.setLastChangeDateInParticipants(now);
        if (comments) task.setLastChangeDateInComments(now);
        touchGroupChange(task.getGroup(), false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasNewInvitations() {
        Long userId = effectiveCurrentUser.getUserId();
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Instant lastReceived = user.getLastInviteReceivedAt();
        if (lastReceived == null) {

            return false;
        }
        Instant lastSeen = user.getLastSeenInvites();

        return lastSeen == null || lastReceived.isAfter(lastSeen);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasGroupChanged(Long groupId, Instant lastSeen) {
        authorizationService.requireAnyRoleIn(groupId);
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with id " + groupId + " not found"));
        boolean groupChanged = group.getLastChangeInGroup() != null && group.getLastChangeInGroup().isAfter(lastSeen);
        boolean tasksDeleted = group.getLastDeleteTaskDate() != null && group.getLastDeleteTaskDate().isAfter(lastSeen);
    return groupChanged || tasksDeleted;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasTaskChanged(Long groupId, Long taskId, Instant since) {
        authorizationService.requireAnyRoleIn(groupId);
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));
        if (!task.getGroup().getId().equals(groupId)) {
            throw new TaskNotFoundException("Task not found in this group");
        }
        return task.getLastChangeDate() != null && task.getLastChangeDate().isAfter(since.plusSeconds(5));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasCommentsChanged(Long groupId, Long taskId, Instant since) {
        authorizationService.requireAnyRoleIn(groupId);
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));
        if (!task.getGroup().getId().equals(groupId)) {
            throw new TaskNotFoundException("Task not found in this group");
        }
        return task.getLastChangeDateInComments() != null && task.getLastChangeDateInComments().isAfter(since);
    }

    @Override
    @Transactional(readOnly = true)
    public void checkMembership(Long groupId) {
        authorizationService.requireAnyRoleIn(groupId);
    }

    private boolean computeIsDeletable(Task task, Role currentUserRole, Long groupId, Long currentUserId) {
        if (currentUserRole == null) return false;
        if (currentUserRole == Role.GROUP_LEADER) return true;
        if (currentUserRole != Role.TASK_MANAGER) return false;
        Long creatorId = task.getCreatorIdSnapshot();
        if (creatorId == null) return true;

        if (creatorId.equals(currentUserId)) return true;
        var creatorMembership = groupMembershipRepository.findByGroupIdAndUserId(groupId, creatorId);
        return creatorMembership.isEmpty() ||
            (creatorMembership.get().getRole() != Role.GROUP_LEADER &&
             creatorMembership.get().getRole() != Role.TASK_MANAGER);
    }

}
