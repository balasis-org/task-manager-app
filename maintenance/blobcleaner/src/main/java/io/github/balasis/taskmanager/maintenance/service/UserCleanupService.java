package io.github.balasis.taskmanager.maintenance.service;

import io.github.balasis.taskmanager.maintenance.base.BaseComponent;
import io.github.balasis.taskmanager.maintenance.repository.MaintenanceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// deletes users who have no group memberships and haven't been active for
// 7 days (no custom image) or 14 days (has custom image — gives more time
// in case they uploaded but haven't joined a group yet).
// FK violations are swallowed — means the user got re-activated mid-cleanup.
@Service
@AllArgsConstructor
public class UserCleanupService extends BaseComponent {

    private final MaintenanceRepository repository;

    public void cleanInactiveUsers() {
        List<Long> ids = repository.findInactiveUserIdsWithoutGroups();

        if (ids.isEmpty()) {
            logger.info("User cleanup: no inactive group-less users found.");
            return;
        }

        logger.info("User cleanup: found {} inactive group-less user(s) to process.", ids.size());
        int deleted = 0;
        int skipped = 0;
        for (Long userId : ids) {
            if (repository.tryDeleteUser(userId)) {
                deleted++;
                logger.info("User cleanup: deleted user id={}", userId);
            } else {
                skipped++;
                logger.warn("User cleanup: skipped user id={} (FK constraint, likely re-activated)", userId);
            }
        }
        logger.info("User cleanup: finished. deleted={}, skipped={}", deleted, skipped);
    }
}
