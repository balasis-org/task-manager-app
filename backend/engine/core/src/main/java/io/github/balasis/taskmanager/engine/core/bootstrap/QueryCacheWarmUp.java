package io.github.balasis.taskmanager.engine.core.bootstrap;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.enumeration.InvitationStatus;
import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.model.BootstrapLock;
import io.github.balasis.taskmanager.engine.core.repository.BootstrapLockRepository;
import io.github.balasis.taskmanager.engine.core.repository.DefaultImageRepository;
import io.github.balasis.taskmanager.engine.core.repository.DeletedTaskRepository;
import io.github.balasis.taskmanager.engine.core.repository.EmailOutboxRepository;
import io.github.balasis.taskmanager.engine.core.repository.FileReviewStatusRepository;
import io.github.balasis.taskmanager.engine.core.repository.GroupEventRepository;
import io.github.balasis.taskmanager.engine.core.repository.GroupInvitationRepository;
import io.github.balasis.taskmanager.engine.core.repository.GroupMembershipRepository;
import io.github.balasis.taskmanager.engine.core.repository.GroupRepository;
import io.github.balasis.taskmanager.engine.core.repository.ImageModerationQueueRepository;
import io.github.balasis.taskmanager.engine.core.repository.RefreshTokenRepository;
import io.github.balasis.taskmanager.engine.core.repository.TaskAnalysisRequestRepository;
import io.github.balasis.taskmanager.engine.core.repository.TaskAnalysisSnapshotRepository;
import io.github.balasis.taskmanager.engine.core.repository.TaskAssigneeFileRepository;
import io.github.balasis.taskmanager.engine.core.repository.TaskCommentRepository;
import io.github.balasis.taskmanager.engine.core.repository.TaskFileRepository;
import io.github.balasis.taskmanager.engine.core.repository.TaskParticipantRepository;
import io.github.balasis.taskmanager.engine.core.repository.TaskRepository;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.infrastructure.bootstrap.StartupGate;
import io.github.balasis.taskmanager.shared.enums.BlobContainerType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Set;

// Pre-warms the SQL Server execution plan cache on boot by firing every
// repository query with non-matching dummy parameters. SELECTs return empty
// results, DML affects 0 rows — zero side effects on real data. Coordinated
// via a DB pessimistic lock (BootstrapLocks table) so only one instance warms
// the shared plan cache in a scale-out deployment. The second instance blocks
// on SELECT FOR UPDATE until warm-up commits, then skips.
@Component
@RequiredArgsConstructor
public class QueryCacheWarmUp extends BaseComponent {

    private static final String LOCK_NAME = "QUERY_CACHE_WARMUP";
    private static final Long DUMMY = -999L;
    private static final String WARMUP = "__warmup__";
    private static final Pageable PAGE = PageRequest.of(0, 1);

    private final BootstrapLockRepository bootstrapLockRepository;
    private final DefaultImageRepository defaultImageRepository;
    private final DeletedTaskRepository deletedTaskRepository;
    private final EmailOutboxRepository emailOutboxRepository;
    private final FileReviewStatusRepository fileReviewStatusRepository;
    private final GroupEventRepository groupEventRepository;
    private final GroupInvitationRepository groupInvitationRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupRepository groupRepository;
    private final ImageModerationQueueRepository imageModerationQueueRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TaskAnalysisRequestRepository taskAnalysisRequestRepository;
    private final TaskAnalysisSnapshotRepository taskAnalysisSnapshotRepository;
    private final TaskAssigneeFileRepository taskAssigneeFileRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskFileRepository taskFileRepository;
    private final TaskParticipantRepository taskParticipantRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    private final StartupGate startupGate;
    private final PlatformTransactionManager txManager;

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void warmUp() {
        try {
            ensureLockRowExists();

            new TransactionTemplate(txManager).executeWithoutResult(status -> {
                BootstrapLock lock = bootstrapLockRepository
                        .findByNameForUpdate(LOCK_NAME).orElseThrow();

                if (lock.isCompleted()) {
                    lock.setCompleted(false);
                    logger.info("SQL plan cache warm-up: skipped — another instance completed it");
                    return;
                }

                logger.info("SQL plan cache warm-up: starting (19 repositories, ~112 queries)");
                long start = System.nanoTime();

                warmUpBootstrapLockRepository();
                warmUpDefaultImageRepository();
                warmUpDeletedTaskRepository();
                warmUpEmailOutboxRepository();
                warmUpFileReviewStatusRepository();
                warmUpGroupEventRepository();
                warmUpGroupInvitationRepository();
                warmUpGroupMembershipRepository();
                warmUpGroupRepository();
                warmUpImageModerationQueueRepository();
                warmUpRefreshTokenRepository();
                warmUpTaskAnalysisRequestRepository();
                warmUpTaskAnalysisSnapshotRepository();
                warmUpTaskAssigneeFileRepository();
                warmUpTaskCommentRepository();
                warmUpTaskFileRepository();
                warmUpTaskParticipantRepository();
                warmUpTaskRepository();
                warmUpUserRepository();

                long ms = (System.nanoTime() - start) / 1_000_000;
                logger.info("SQL plan cache warm-up: completed in {} ms", ms);

                lock.setCompleted(true);
            });
        } catch (Exception e) {
            logger.warn("SQL plan cache warm-up failed — proceeding without warm cache: {}",
                    e.getMessage());
        } finally {
            startupGate.markQueryCacheReady();
        }
    }

    private void ensureLockRowExists() {
        if (bootstrapLockRepository.findByName(LOCK_NAME).isPresent()) return;
        try {
            new TransactionTemplate(txManager).executeWithoutResult(s ->
                    bootstrapLockRepository.saveAndFlush(
                            BootstrapLock.builder().name(LOCK_NAME).completed(false).build()));
        } catch (DataIntegrityViolationException ignored) { }
    }

    // ── Per-repository warm-up methods ──────────────────────────────────

    private void warmUpBootstrapLockRepository() {
        logger.info("  Warming: BootstrapLockRepository");
        try {
            bootstrapLockRepository.findByName(WARMUP);
            // findByNameForUpdate already warmed by the lock acquisition above
        } catch (Exception e) { logger.debug("  Incomplete: BootstrapLockRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  BootstrapLockRepository");
    }

    private void warmUpDefaultImageRepository() {
        logger.info("  Warming: DefaultImageRepository");
        try {
            defaultImageRepository.findByType(BlobContainerType.PROFILE_IMAGES);
            defaultImageRepository.existsByTypeAndFileName(BlobContainerType.PROFILE_IMAGES, WARMUP);
        } catch (Exception e) { logger.debug("  Incomplete: DefaultImageRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  DefaultImageRepository");
    }

    private void warmUpDeletedTaskRepository() {
        logger.info("  Warming: DeletedTaskRepository");
        try {
            deletedTaskRepository.findDeletedTaskIdsByGroupIdAndDeletedAtAfter(DUMMY, Instant.EPOCH);
            deletedTaskRepository.deleteAllByGroup_Id(DUMMY);
        } catch (Exception e) { logger.debug("  Incomplete: DeletedTaskRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  DeletedTaskRepository");
    }

    private void warmUpEmailOutboxRepository() {
        logger.info("  Warming: EmailOutboxRepository");
        try {
            emailOutboxRepository.findPendingBatch(PAGE);
            emailOutboxRepository.countSentSince(Instant.EPOCH);
            emailOutboxRepository.markSent(DUMMY, Instant.now());
            emailOutboxRepository.incrementRetryOrFail(DUMMY);
        } catch (Exception e) { logger.debug("  Incomplete: EmailOutboxRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  EmailOutboxRepository");
    }

    private void warmUpFileReviewStatusRepository() {
        logger.info("  Warming: FileReviewStatusRepository");
        try {
            fileReviewStatusRepository.findByTaskFileIdIn(Set.of(DUMMY));
            fileReviewStatusRepository.findByTaskAssigneeFileIdIn(Set.of(DUMMY));
            fileReviewStatusRepository.findByTaskFile_IdAndReviewer_Id(DUMMY, DUMMY);
            fileReviewStatusRepository.findByTaskAssigneeFile_IdAndReviewer_Id(DUMMY, DUMMY);
            fileReviewStatusRepository.deleteByTaskFileId(DUMMY);
            fileReviewStatusRepository.deleteByTaskAssigneeFileId(DUMMY);
            fileReviewStatusRepository.deleteAllByTaskFileGroupId(DUMMY);
            fileReviewStatusRepository.deleteAllByTaskAssigneeFileGroupId(DUMMY);
            fileReviewStatusRepository.deleteAllByTaskFileTaskId(DUMMY);
            fileReviewStatusRepository.deleteAllByTaskAssigneeFileTaskId(DUMMY);
        } catch (Exception e) { logger.debug("  Incomplete: FileReviewStatusRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  FileReviewStatusRepository");
    }

    private void warmUpGroupEventRepository() {
        logger.info("  Warming: GroupEventRepository");
        try {
            groupEventRepository.findAllByGroup_Id(DUMMY, PAGE);
            groupEventRepository.deleteAllByGroup_Id(DUMMY);
        } catch (Exception e) { logger.debug("  Incomplete: GroupEventRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  GroupEventRepository");
    }

    private void warmUpGroupInvitationRepository() {
        logger.info("  Warming: GroupInvitationRepository");
        try {
            groupInvitationRepository.existsByUser_IdAndGroup_Id(DUMMY, DUMMY);
            groupInvitationRepository.findByIdWithGroup(DUMMY);
            groupInvitationRepository.findByUser_IdAndInvitationStatus(DUMMY, InvitationStatus.PENDING);
            groupInvitationRepository.existsByUser_IdAndGroup_IdAndInvitationStatus(
                    DUMMY, DUMMY, InvitationStatus.PENDING);
            groupInvitationRepository.findIncomingByUserIdAndStatusWithFetch(
                    DUMMY, InvitationStatus.PENDING);
            groupInvitationRepository.findAllSentByInvitedByIdWithFetch(DUMMY);
            groupInvitationRepository.findAllSentByInvitedByIdAndStatusWithFetch(
                    DUMMY, InvitationStatus.PENDING);
            groupInvitationRepository.existsByUser_IdAndInvitationStatusAndCreatedAtAfter(
                    DUMMY, InvitationStatus.PENDING, Instant.EPOCH);
            groupInvitationRepository.existsByUser_IdAndInvitationStatus(
                    DUMMY, InvitationStatus.PENDING);
            groupInvitationRepository.deleteAllByGroup_Id(DUMMY);
            groupInvitationRepository.deleteAllByUser_IdOrInvitedBy_Id(DUMMY, DUMMY);
            groupInvitationRepository.findTopByGroup_IdAndUser_IdAndInvitationStatusOrderByIdDesc(
                    DUMMY, DUMMY, InvitationStatus.PENDING);
        } catch (Exception e) { logger.debug("  Incomplete: GroupInvitationRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  GroupInvitationRepository");
    }

    private void warmUpGroupMembershipRepository() {
        logger.info("  Warming: GroupMembershipRepository");
        try {
            groupMembershipRepository.findByUserIdAndGroupId(DUMMY, DUMMY);
            groupMembershipRepository.findByUserIdWithGroup(DUMMY);
            groupMembershipRepository.existsByGroupIdAndUserId(DUMMY, DUMMY);
            groupMembershipRepository.findByGroupIdAndUserId(DUMMY, DUMMY);
            groupMembershipRepository.deleteAllByGroup_Id(DUMMY);
            groupMembershipRepository.findByGroup_Id(DUMMY, PAGE);
            groupMembershipRepository.findByIdWithUser(DUMMY);
            groupMembershipRepository.searchByGroupIdAndUser(DUMMY, WARMUP, PAGE);
            groupMembershipRepository.deleteByGroupIdAndUserId(DUMMY, DUMMY);
            groupMembershipRepository.findByGroup_IdAndRole(DUMMY, Role.GUEST);
            groupMembershipRepository.findByUser_IdAndGroup_Id(DUMMY, DUMMY);
            groupMembershipRepository.countByUser_Id(DUMMY);
            groupMembershipRepository.countByGroup_Id(DUMMY);
        } catch (Exception e) { logger.debug("  Incomplete: GroupMembershipRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  GroupMembershipRepository");
    }

    private void warmUpGroupRepository() {
        logger.info("  Warming: GroupRepository");
        try {
            groupRepository.existsByNameAndOwner_Id(WARMUP, DUMMY);
            groupRepository.existsByNameAndOwner_IdAndIdNot(WARMUP, DUMMY, DUMMY);
            groupRepository.countByOwner_Id(DUMMY);
            groupRepository.findByIdWithTasksAndParticipants(DUMMY);
            groupRepository.adminFindByIdWithDetails(DUMMY);
            groupRepository.findByIdWithOwner(DUMMY);
            groupRepository.adminSearchGroups(WARMUP, PAGE);
            groupRepository.adminFindAllGroups(PAGE);
            groupRepository.touchLastChangeByOwnerId(DUMMY, Instant.now());
        } catch (Exception e) { logger.debug("  Incomplete: GroupRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  GroupRepository");
    }

    private void warmUpImageModerationQueueRepository() {
        logger.info("  Warming: ImageModerationQueueRepository");
        try {
            imageModerationQueueRepository.findPendingBatch(PAGE);
            imageModerationQueueRepository.countViolationsSince(DUMMY, Instant.EPOCH);
            imageModerationQueueRepository.incrementRetryOrFail(DUMMY);
        } catch (Exception e) { logger.debug("  Incomplete: ImageModerationQueueRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  ImageModerationQueueRepository");
    }

    private void warmUpRefreshTokenRepository() {
        logger.info("  Warming: RefreshTokenRepository");
        try {
            refreshTokenRepository.deleteAllByUser_Id(DUMMY);
        } catch (Exception e) { logger.debug("  Incomplete: RefreshTokenRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  RefreshTokenRepository");
    }

    private void warmUpTaskAnalysisRequestRepository() {
        logger.info("  Warming: TaskAnalysisRequestRepository");
        try {
            taskAnalysisRequestRepository.findByStatus(WARMUP, PAGE);
            taskAnalysisRequestRepository.existsByTaskIdAndStatusIn(DUMMY, List.of(WARMUP));
            taskAnalysisRequestRepository.casUpdateStatus(DUMMY, WARMUP, WARMUP, Instant.now());
            taskAnalysisRequestRepository.recoverStaleProcessing(Instant.EPOCH);
        } catch (Exception e) { logger.debug("  Incomplete: TaskAnalysisRequestRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  TaskAnalysisRequestRepository");
    }

    private void warmUpTaskAnalysisSnapshotRepository() {
        logger.info("  Warming: TaskAnalysisSnapshotRepository");
        try {
            taskAnalysisSnapshotRepository.findByTaskId(DUMMY);
        } catch (Exception e) { logger.debug("  Incomplete: TaskAnalysisSnapshotRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  TaskAnalysisSnapshotRepository");
    }

    private void warmUpTaskAssigneeFileRepository() {
        logger.info("  Warming: TaskAssigneeFileRepository");
        try {
            taskAssigneeFileRepository.findAllByGroupId(DUMMY);
            taskAssigneeFileRepository.findAllByGroupIdAndParticipant(DUMMY, DUMMY);
        } catch (Exception e) { logger.debug("  Incomplete: TaskAssigneeFileRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  TaskAssigneeFileRepository");
    }

    private void warmUpTaskCommentRepository() {
        logger.info("  Warming: TaskCommentRepository");
        try {
            taskCommentRepository.countByTask_Id(DUMMY);
            taskCommentRepository.findAllByTask_id(DUMMY, PAGE);
            taskCommentRepository.findWithCreatorById(DUMMY);
            taskCommentRepository.findByIdWithTaskAndCreator(DUMMY);
            taskCommentRepository.detachCreatorFromGroupComments(DUMMY, DUMMY, WARMUP);
            taskCommentRepository.detachCreatorFromAllComments(DUMMY, WARMUP);
            taskCommentRepository.adminFilterComments(DUMMY, DUMMY, DUMMY, PAGE);
            taskCommentRepository.deleteByTaskIdAndCreatedAtBefore(DUMMY, Instant.EPOCH);
            taskCommentRepository.countAndSumCharsByTaskId(DUMMY);
        } catch (Exception e) { logger.debug("  Incomplete: TaskCommentRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  TaskCommentRepository");
    }

    private void warmUpTaskFileRepository() {
        logger.info("  Warming: TaskFileRepository");
        try {
            taskFileRepository.findAllByGroupId(DUMMY);
            taskFileRepository.findAllByGroupIdAndParticipant(DUMMY, DUMMY);
        } catch (Exception e) { logger.debug("  Incomplete: TaskFileRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  TaskFileRepository");
    }

    private void warmUpTaskParticipantRepository() {
        logger.info("  Warming: TaskParticipantRepository");
        try {
            taskParticipantRepository.deleteByUserIdAndGroupId(DUMMY, DUMMY);
            taskParticipantRepository.deleteReviewersByUserIdAndGroupId(DUMMY, DUMMY);
            taskParticipantRepository.findAllByTask_idAndUser_id(DUMMY, DUMMY);
        } catch (Exception e) { logger.debug("  Incomplete: TaskParticipantRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  TaskParticipantRepository");
    }

    private void warmUpTaskRepository() {
        logger.info("  Warming: TaskRepository");
        try {
            taskRepository.existsByTitleAndGroup_Id(WARMUP, DUMMY);
            taskRepository.existsByTitleAndGroup_IdAndIdNot(WARMUP, DUMMY, DUMMY);
            taskRepository.countByGroup_Id(DUMMY);
            taskRepository.deleteAllByGroup_Id(DUMMY);
            taskRepository.findByIdWithFullFetchParticipantsAndFiles(DUMMY);
            taskRepository.findByIdWithParticipantsAndFiles(DUMMY);
            taskRepository.findByIdWithTaskParticipants(DUMMY);
            taskRepository.findByIdWithFiles(DUMMY);
            taskRepository.findByIdWithFilesAndGroup(DUMMY);
            taskRepository.searchBy(DUMMY, DUMMY, null, null, null);
            taskRepository.searchTasksForPreviewWithFilters(DUMMY, null, null, null, null, null);
            taskRepository.findChangedSince(DUMMY, Instant.EPOCH);
            taskRepository.filterTaskIds(DUMMY, null, null, null, null, null, null, null, null, null);
            taskRepository.nullifyReviewedByForUser(DUMMY);
            taskRepository.nullifyLastEditByForUser(DUMMY);
            taskRepository.adminSearchTasks(WARMUP, PAGE);
            taskRepository.adminFindAllTasks(PAGE);
        } catch (Exception e) { logger.debug("  Incomplete: TaskRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  TaskRepository");
    }

    private void warmUpUserRepository() {
        logger.info("  Warming: UserRepository");
        try {
            userRepository.findByAzureKey(WARMUP);
            userRepository.findByEmail(WARMUP);
            userRepository.existsByAzureKey(WARMUP);
            userRepository.findByInviteCode(WARMUP);
            userRepository.addStorageUsage(DUMMY, 0L, Long.MAX_VALUE);
            userRepository.subtractStorageUsage(DUMMY, 0L);
            userRepository.addDownloadUsage(DUMMY, 0L, Long.MAX_VALUE);
            userRepository.incrementEmailUsage(DUMMY, Integer.MAX_VALUE);
            userRepository.incrementImageScanUsage(DUMMY, Integer.MAX_VALUE);
            userRepository.decrementImageScanUsage(DUMMY);
            userRepository.incrementTaskAnalysisCredits(DUMMY, 0, Integer.MAX_VALUE);
            userRepository.decrementTaskAnalysisCredits(DUMMY, 0);
            userRepository.searchUser(WARMUP, PAGE);
            userRepository.searchUserForInvites(DUMMY, WARMUP, WARMUP, PAGE);
        } catch (Exception e) { logger.debug("  Incomplete: UserRepository — {}", e.getMessage()); }
        logger.info("  Warmed:  UserRepository");
    }
}
