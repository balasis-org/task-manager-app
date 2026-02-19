package io.github.balasis.taskmanager.maintenance.service;

import io.github.balasis.taskmanager.maintenance.base.BaseComponent;
import io.github.balasis.taskmanager.maintenance.repository.MaintenanceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UserCleanupService extends BaseComponent {

    private final MaintenanceRepository repository;

    /**
     * Deletes users who do not belong to any group and have been inactive
     * for more than 7 days (no profile image) or 14 days (has profile image).
     */
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
                logger.warn("User cleanup: skipped user id={} (FK constraint — likely re-activated)", userId);
            }
        }
        logger.info("User cleanup: finished. deleted={}, skipped={}", deleted, skipped);
    }
}
