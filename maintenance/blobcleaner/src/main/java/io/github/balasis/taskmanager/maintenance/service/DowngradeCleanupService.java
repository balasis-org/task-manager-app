package io.github.balasis.taskmanager.maintenance.service;

import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.maintenance.base.BaseComponent;
import io.github.balasis.taskmanager.maintenance.repository.MaintenanceRepository;
import io.github.balasis.taskmanager.maintenance.repository.MaintenanceRepository.DowngradeFileRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

// Cleans up over-budget files for users whose downgrade grace period expired.
// Deletion priority: unshielded groups first, DONE tasks first, oldest files first.
// After cleanup (or if already within budget), grace fields are cleared.
@Service
@RequiredArgsConstructor
public class DowngradeCleanupService extends BaseComponent {

    private final MaintenanceRepository maintenanceRepository;
    private final BlobAccessService blobAccessService;

    // Maps PlanLimits.storageBudgetBytes values — duplicated here because
    // the maintenance module doesn't depend on context-base. Keep in sync.
    private static final Map<String, Long> STORAGE_BUDGETS = Map.of(
            "FREE",      100L * 1024 * 1024,
            "STUDENT",   500L * 1024 * 1024,
            "ORGANIZER", 2L   * 1024 * 1024 * 1024,
            "TEAM",      5L   * 1024 * 1024 * 1024,
            "TEAMS_PRO", 5L   * 1024 * 1024 * 1024
    );

    public void cleanGraceExpiredUsers() {
        List<Long> expiredUserIds = maintenanceRepository.findGraceExpiredUserIds();
        if (expiredUserIds.isEmpty()) {
            logger.info("Downgrade cleanup: no grace-expired users.");
            return;
        }

        logger.info("Downgrade cleanup: processing {} user(s) with expired grace periods.", expiredUserIds.size());

        for (Long userId : expiredUserIds) {
            try {
                cleanUserFiles(userId);
            } catch (Exception e) {
                logger.error("Downgrade cleanup failed for user {}: {}", userId, e.getMessage());
            }
            maintenanceRepository.clearGraceFields(userId);
        }
    }

    private void cleanUserFiles(long userId) {
        long usedBytes = maintenanceRepository.getUsedStorageBytes(userId);
        String plan = getCurrentPlan(userId);
        long budget = STORAGE_BUDGETS.getOrDefault(plan, 0L);

        if (budget <= 0) {
            // Unknown plan — nothing to enforce. Clear grace fields and move on.
            logger.info("User {} has plan {} with no mapped budget, clearing grace fields only.", userId, plan);
            return;
        }

        if (usedBytes <= budget) {
            logger.info("User {} is within budget ({}/{} bytes), clearing grace fields only.", userId, usedBytes, budget);
            return;
        }

        long excessBytes = usedBytes - budget;
        logger.info("User {} is {} bytes over budget, deleting files...", userId, excessBytes);

        List<DowngradeFileRow> files = maintenanceRepository.findDeletableFilesByPriority(userId);
        long deletedBytes = 0;
        int deletedCount = 0;

        for (DowngradeFileRow file : files) {
            if (deletedBytes >= excessBytes) break;

            try {
                BlobContainerType container = "CREATOR".equals(file.fileType())
                        ? BlobContainerType.TASK_FILES
                        : BlobContainerType.TASK_ASSIGNEE_FILES;

                blobAccessService.deleteBlob(container, file.fileUrl());

                if ("CREATOR".equals(file.fileType())) {
                    maintenanceRepository.deleteCreatorFile(file.fileId());
                } else {
                    maintenanceRepository.deleteAssigneeFile(file.fileId());
                }

                deletedBytes += file.fileSize();
                deletedCount++;
            } catch (Exception e) {
                logger.warn("Failed to delete file {} (type={}): {}", file.fileId(), file.fileType(), e.getMessage());
            }
        }

        // Reconcile the user's storage counter after bulk deletion
        maintenanceRepository.reconcileStorageBudgets();
        logger.info("User {}: deleted {} file(s), freed {} bytes.", userId, deletedCount, deletedBytes);
    }

    private String getCurrentPlan(long userId) {
        try {
            return maintenanceRepository.getJdbcTemplate().queryForObject(
                    "SELECT subscriptionPlan FROM Users WHERE id = ?", String.class, userId);
        } catch (Exception e) {
            return "FREE";
        }
    }
}
