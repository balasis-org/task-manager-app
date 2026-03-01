package io.github.balasis.taskmanager.maintenance.service;

import io.github.balasis.taskmanager.maintenance.base.BaseComponent;
import io.github.balasis.taskmanager.maintenance.repository.MaintenanceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DatabaseCleanupService extends BaseComponent {

    private static final int TOMBSTONE_RETENTION_DAYS = 30;
    private static final int INVITATION_RETENTION_DAYS = 30;

    private final MaintenanceRepository repository;

    public void cleanStaleRows() {
        purgeExpiredTokens();
        purgeDeletedTaskTombstones();
        purgeResolvedInvitations();
    }

    private void purgeExpiredTokens() {
        int deleted = repository.purgeExpiredRefreshTokens();
        if (deleted > 0) {
            logger.info("DB cleanup: purged {} expired refresh token(s).", deleted);
        } else {
            logger.info("DB cleanup: no expired refresh tokens found.");
        }
    }

    private void purgeDeletedTaskTombstones() {
        int deleted = repository.purgeOldDeletedTasks(TOMBSTONE_RETENTION_DAYS);
        if (deleted > 0) {
            logger.info("DB cleanup: purged {} deleted-task tombstone(s) older than {} days.",
                    deleted, TOMBSTONE_RETENTION_DAYS);
        } else {
            logger.info("DB cleanup: no stale deleted-task tombstones found.");
        }
    }

    private void purgeResolvedInvitations() {
        int deleted = repository.purgeResolvedInvitations(INVITATION_RETENTION_DAYS);
        if (deleted > 0) {
            logger.info("DB cleanup: purged {} resolved invitation(s) older than {} days.",
                    deleted, INVITATION_RETENTION_DAYS);
        } else {
            logger.info("DB cleanup: no stale resolved invitations found.");
        }
    }
}
