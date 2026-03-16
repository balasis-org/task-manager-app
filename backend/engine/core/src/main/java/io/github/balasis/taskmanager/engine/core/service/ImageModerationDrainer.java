package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.model.ImageModerationQueue;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.engine.core.repository.ImageModerationQueueRepository;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.infrastructure.blob.service.BlobStorageService;
import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.ContentSafetyService;
import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.ModerationResult;
import io.github.balasis.taskmanager.engine.infrastructure.redis.ImageModerationLockService;
import io.github.balasis.taskmanager.engine.core.repository.GroupRepository;
import io.github.balasis.taskmanager.context.base.model.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Profile({"prod-h2", "prod-azuresql", "dev-h2", "dev-mssql", "dev-flyway-mssql"})
public class ImageModerationDrainer {

    private static final Logger logger = LoggerFactory.getLogger(ImageModerationDrainer.class);

    private static final int BATCH_SIZE = 5;
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    // Escalation thresholds
    private static final int VIOLATIONS_PER_LEVEL = 3;
    private static final Duration ESCALATION_WINDOW = Duration.ofDays(30);

    // Ban durations per level (1-indexed: level 1..4)
    private static final Duration[] BAN_DURATIONS = {
            Duration.ofHours(1),   // level 1
            Duration.ofHours(24),  // level 2
            Duration.ofDays(7),    // level 3
            Duration.ofDays(30)    // level 4 — account write ban
    };

    private final ImageModerationQueueRepository queueRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ContentSafetyService contentSafetyService;
    private final BlobStorageService blobStorageService;
    private final ImageModerationLockService lockService;
    private final TransactionTemplate txTemplate;

    public ImageModerationDrainer(
            ImageModerationQueueRepository queueRepository,
            UserRepository userRepository,
            GroupRepository groupRepository,
            ContentSafetyService contentSafetyService,
            BlobStorageService blobStorageService,
            ImageModerationLockService lockService,
            PlatformTransactionManager txManager) {
        this.queueRepository = queueRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.contentSafetyService = contentSafetyService;
        this.blobStorageService = blobStorageService;
        this.lockService = lockService;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    @Scheduled(fixedDelay = 10_000)
    public void drain() {
        if (!lockService.tryAcquireLock(LOCK_TTL)) return;
        try {
            List<ImageModerationQueue> batch = queueRepository.findPendingBatch(
                    PageRequest.of(0, BATCH_SIZE));
            for (ImageModerationQueue item : batch) {
                processSingle(item);
            }
        } finally {
            lockService.releaseLock();
        }
    }

    private void processSingle(ImageModerationQueue item) {
        try {
            byte[] imageBytes = blobStorageService.downloadBlobBytes(
                    item.getEntityType(), item.getNewBlobName());

            if (imageBytes == null) {
                txTemplate.executeWithoutResult(status -> {
                    item.setStatus("FAILED");
                    item.setProcessedAt(Instant.now());
                    queueRepository.save(item);
                });
                return;
            }

            ModerationResult result = contentSafetyService.analyze(
                    new ByteArrayInputStream(imageBytes));

            txTemplate.executeWithoutResult(status -> {
                if (result.isSafe()) {
                    handleApproval(item);
                } else {
                    handleRejection(item, result);
                }
            });
        } catch (Exception e) {
            logger.warn("Moderation failed for queue id={}: {}", item.getId(), e.getMessage());
            queueRepository.incrementRetryOrFail(item.getId());
        }
    }

    private void handleApproval(ImageModerationQueue item) {
        item.setStatus("APPROVED");
        item.setProcessedAt(Instant.now());
        queueRepository.save(item);

        // delete old blob — the new one is confirmed safe
        if (item.getPreviousBlobName() != null) {
            deleteBlobSafe(item.getEntityType(), item.getPreviousBlobName());
        }

        logger.debug("Moderation APPROVED: queue id={}, entity={}:{}",
                item.getId(), item.getEntityType(), item.getEntityId());
    }

    private void handleRejection(ImageModerationQueue item, ModerationResult result) {
        item.setStatus("REJECTED");
        item.setRejectedCategory(result.rejectedCategory());
        item.setRejectedSeverity(result.rejectedSeverity());
        item.setProcessedAt(Instant.now());
        queueRepository.save(item);

        // delete offending blob
        deleteBlobSafe(item.getEntityType(), item.getNewBlobName());

        // revert entity's imgUrl to previous
        revertImage(item);

        // refund the image scan charge — user should not lose quota for rejected content
        userRepository.decrementImageScanUsage(item.getUserId());

        // apply escalation
        applyEscalation(item.getUserId());

        logger.warn("Moderation REJECTED: queue id={}, entity={}:{}, category={}, severity={}",
                item.getId(), item.getEntityType(), item.getEntityId(),
                result.rejectedCategory(), result.rejectedSeverity());
    }

    private void revertImage(ImageModerationQueue item) {
        if ("USER".equals(item.getEntityType())) {
            userRepository.findById(item.getEntityId()).ifPresent(user -> {
                user.setImgUrl(item.getPreviousBlobName());
                userRepository.save(user);
            });
        } else if ("GROUP".equals(item.getEntityType())) {
            groupRepository.findById(item.getEntityId()).ifPresent(group -> {
                group.setImgUrl(item.getPreviousBlobName());
                groupRepository.save(group);
            });
        }
    }

    private void applyEscalation(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        Instant now = Instant.now();
        Instant windowStart = now.minus(ESCALATION_WINDOW);

        // If >30 days since last ban, reset to level 0
        if (user.getLastUploadBanAt() != null
                && user.getLastUploadBanAt().isBefore(windowStart)) {
            user.setUploadBanCount(0);
        }

        // Count violations since last ban (or 30 days ago)
        Instant countSince = user.getLastUploadBanAt() != null
                ? user.getLastUploadBanAt()
                : windowStart;
        // use the more recent of the two
        if (countSince.isBefore(windowStart)) {
            countSince = windowStart;
        }

        int violations = queueRepository.countViolationsSince(userId, countSince);

        if (violations >= VIOLATIONS_PER_LEVEL) {
            int currentLevel = user.getUploadBanCount();
            int newLevel = Math.min(currentLevel + 1, BAN_DURATIONS.length);
            Duration banDuration = BAN_DURATIONS[newLevel - 1];

            if (newLevel < BAN_DURATIONS.length) {
                // Levels 1-3: upload prevention
                user.setUploadBannedUntil(now.plus(banDuration));
            } else {
                // Level 4: account write ban + clear level
                user.setAccountBannedUntil(now.plus(banDuration));
                newLevel = 0; // reset for fresh start after punishment
            }

            user.setUploadBanCount(newLevel);
            user.setLastUploadBanAt(now);

            logger.warn("Escalation applied: userId={}, newLevel={}, banDuration={}",
                    userId, newLevel, banDuration);
        }

        userRepository.save(user);
    }

    private void deleteBlobSafe(String entityType, String blobName) {
        try {
            if ("USER".equals(entityType)) {
                blobStorageService.deleteProfileImage(blobName);
            } else if ("GROUP".equals(entityType)) {
                blobStorageService.deleteGroupImage(blobName);
            }
        } catch (Exception e) {
            logger.warn("Failed to delete blob {}: {}", blobName, e.getMessage());
        }
    }
}
