package io.github.balasis.taskmanager.maintenance.repository;

import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@AllArgsConstructor
public class MaintenanceRepository {

    @lombok.Getter
    private final JdbcTemplate jdbcTemplate;

    public boolean existsById(BlobContainerType type, long id) {
        String sql = String.format(
                "SELECT COUNT(*) FROM %s WHERE id = ?",
                type.getTableName()
        );

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    public boolean existsByBlobName(BlobContainerType type, String blobName) {
        String sql = String.format(
                "SELECT COUNT(*) FROM %s WHERE %s = ?",
                type.getTableName(), type.getColumnName()
        );

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, blobName);
        return count != null && count > 0;
    }

    public List<Long> findInactiveUserIdsWithoutGroups() {
        return jdbcTemplate.queryForList("""
            SELECT u.id
            FROM Users u
            WHERE NOT EXISTS (
                SELECT 1 FROM GroupMemberships gm WHERE gm.user_id = u.id
            )
            AND (u.systemRole IS NULL OR u.systemRole <> 'ADMIN')
            AND (
                (u.imgUrl IS NULL AND COALESCE(u.defaultImgUrl, 'profile1.png') = 'profile1.png'
                    AND u.lastActiveAt < DATEADD(DAY, -7,  GETUTCDATE()))
                OR
                ((u.imgUrl IS NOT NULL OR COALESCE(u.defaultImgUrl, 'profile1.png') <> 'profile1.png')
                    AND u.lastActiveAt < DATEADD(DAY, -14, GETUTCDATE()))
            )
        """, Long.class);
    }

    public boolean tryDeleteUser(Long userId) {
        try {

            jdbcTemplate.update("DELETE FROM GroupInvitations WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM GroupInvitations WHERE invited_by_id = ?", userId);
            jdbcTemplate.update("DELETE FROM RefreshTokens    WHERE user_id = ?", userId);

            jdbcTemplate.update("DELETE FROM Users WHERE id = ?", userId);
            return true;
        } catch (DataAccessException e) {

            return false;
        }
    }

    // expired > 1 day ago — rows pile up because logout only deletes the current token
    public int purgeExpiredRefreshTokens() {
        return jdbcTemplate.update("""
            DELETE FROM RefreshTokens
            WHERE expiresAt < DATEADD(DAY, -1, GETUTCDATE())
        """);
    }

    // tombstones older than retentionDays — frontend never queries them after sync window
    public int purgeOldDeletedTasks(int retentionDays) {
        return jdbcTemplate.update("""
            DELETE FROM DeletedTasks
            WHERE deletedAt < DATEADD(DAY, ?, GETUTCDATE())
        """, -retentionDays);
    }

    public int purgeResolvedInvitations(int retentionDays) {
        return jdbcTemplate.update("""
            DELETE FROM GroupInvitations
            WHERE invitationStatus <> 'PENDING'
              AND createdAt < DATEADD(DAY, ?, GETUTCDATE())
        """, -retentionDays);
    }

    // upserts row id=1 — blob-only or full-run timestamp depending on mode
    public void upsertMaintenanceStatus(boolean isFull, int orphanCount, int blobsScanned,
                                        long intervalHours) {
        if (isFull) {
            jdbcTemplate.update("""
                MERGE MaintenanceStatus AS tgt
                USING (SELECT 1 AS id) AS src ON tgt.id = src.id
                WHEN MATCHED THEN
                    UPDATE SET lastFullCleanupAt  = SYSUTCDATETIME(),
                               lastOrphanCount    = ?,
                               lastBlobsScanned   = ?,
                               nextResetAt        = DATEADD(HOUR, ?, SYSUTCDATETIME())
                WHEN NOT MATCHED THEN
                    INSERT (id, lastFullCleanupAt, lastOrphanCount, lastBlobsScanned, nextResetAt)
                    VALUES (1,  SYSUTCDATETIME(), ?, ?, DATEADD(HOUR, ?, SYSUTCDATETIME()));
            """, orphanCount, blobsScanned, (int) intervalHours,
                 orphanCount, blobsScanned, (int) intervalHours);
        } else {
            jdbcTemplate.update("""
                MERGE MaintenanceStatus AS tgt
                USING (SELECT 1 AS id) AS src ON tgt.id = src.id
                WHEN MATCHED THEN
                    UPDATE SET lastBlobCleanupAt  = SYSUTCDATETIME(),
                               lastOrphanCount    = ?,
                               lastBlobsScanned   = ?
                WHEN NOT MATCHED THEN
                    INSERT (id, lastBlobCleanupAt, lastOrphanCount, lastBlobsScanned)
                    VALUES (1,  SYSUTCDATETIME(), ?, ?);
            """, orphanCount, blobsScanned, orphanCount, blobsScanned);
        }
    }

    public int resetGroupCreationCounters() {
        return jdbcTemplate.update("""
            UPDATE Users
            SET totalGroupsCreated       = 0,
                groupCreationBudgetResetAt = SYSUTCDATETIME()
            WHERE totalGroupsCreated > 0
        """);
    }

    // re-derive usedStorageBytes from actual file rows — the atomic
    // inc/dec on upload/delete can drift after crashes or manual DB fixes
    public int reconcileStorageBudgets() {
        return jdbcTemplate.update("""
            WITH actual AS (
                SELECT g.owner_id AS userId,
                       COALESCE(SUM(f.fileSize), 0) AS realBytes
                FROM Groups g
                LEFT JOIN Tasks t      ON t.group_id = g.id
                LEFT JOIN TaskFiles f  ON f.task_id  = t.id
                GROUP BY g.owner_id

                UNION ALL

                SELECT g.owner_id AS userId,
                       COALESCE(SUM(af.fileSize), 0) AS realBytes
                FROM Groups g
                LEFT JOIN Tasks t             ON t.group_id = g.id
                LEFT JOIN TaskAssigneeFiles af ON af.task_id  = t.id
                GROUP BY g.owner_id
            ),
            totals AS (
                SELECT userId, SUM(realBytes) AS totalBytes
                FROM actual
                GROUP BY userId
            )
            UPDATE u
            SET u.usedStorageBytes = t.totalBytes
            FROM Users u
            JOIN totals t ON t.userId = u.id
            WHERE u.usedStorageBytes <> t.totalBytes
        """);
    }

    // only touches rows with non-zero counters
    public int resetMonthlyBudgets() {
        return jdbcTemplate.update("""
            UPDATE Users
            SET usedDownloadBytesMonth       = 0,
                usedEmailsMonth              = 0,
                usedImageScansMonth          = 0,
                usedTaskAnalysisCreditsMonth = 0
            WHERE usedDownloadBytesMonth > 0
               OR usedEmailsMonth > 0
               OR usedImageScansMonth > 0
               OR usedTaskAnalysisCreditsMonth > 0
        """);
    }

    public int purgeProcessedEmails(int retentionHours) {
        return jdbcTemplate.update("""
            DELETE FROM EmailOutbox
            WHERE status IN ('SENT', 'FAILED')
              AND COALESCE(sentAt, createdAt) < DATEADD(HOUR, ?, GETUTCDATE())
        """, -retentionHours);
    }

    public int purgeProcessedModerations(int retentionDays) {
        return jdbcTemplate.update("""
            DELETE FROM ImageModerationQueue
            WHERE status IN ('APPROVED', 'REJECTED', 'FAILED')
              AND COALESCE(processedAt, createdAt) < DATEADD(DAY, ?, GETUTCDATE())
        """, -retentionDays);
    }

    public int purgeCompletedAnalysisRequests(int retentionDays) {
        return jdbcTemplate.update("""
            DELETE FROM TaskAnalysisRequests
            WHERE status IN ('COMPLETED', 'FAILED')
              AND processedAt < DATEADD(DAY, ?, GETUTCDATE())
        """, -retentionDays);
    }

    // ── Downgrade grace-period cleanup ──────────────────────────────────

    // Returns user IDs whose 7-day downgrade grace window has already passed.
    public List<Long> findGraceExpiredUserIds() {
        return jdbcTemplate.queryForList("""
            SELECT id FROM Users
            WHERE downgradeGraceDeadline IS NOT NULL
              AND downgradeGraceDeadline < GETUTCDATE()
        """, Long.class);
    }

    // Returns blob URLs for a user, ordered by cleanup priority:
    // 1. unshielded groups before shielded
    // 2. DONE tasks before non-DONE
    // 3. oldest files first
    // Caller decides how many to actually delete based on byte overage.
    public List<DowngradeFileRow> findDeletableFilesByPriority(long userId) {
        return jdbcTemplate.query("""
            SELECT fileId, fileUrl, fileSize, fileType, shielded, isDone
            FROM (
                SELECT f.id       AS fileId,
                       f.fileUrl  AS fileUrl,
                       f.fileSize AS fileSize,
                       'CREATOR'  AS fileType,
                       CASE WHEN COALESCE(g.downgradeShielded, 0) = 1 THEN 1 ELSE 0 END AS shielded,
                       CASE WHEN t.taskState = 'DONE' THEN 0 ELSE 1 END AS isDone
                FROM TaskFiles f
                JOIN Tasks t  ON t.id       = f.task_id
                JOIN Groups g ON g.id       = t.group_id
                WHERE g.owner_id = ?

                UNION ALL

                SELECT af.id       AS fileId,
                       af.fileUrl  AS fileUrl,
                       af.fileSize AS fileSize,
                       'ASSIGNEE'  AS fileType,
                       CASE WHEN COALESCE(g.downgradeShielded, 0) = 1 THEN 1 ELSE 0 END AS shielded,
                       CASE WHEN t.taskState = 'DONE' THEN 0 ELSE 1 END AS isDone
                FROM TaskAssigneeFiles af
                JOIN Tasks t  ON t.id       = af.task_id
                JOIN Groups g ON g.id       = t.group_id
                WHERE g.owner_id = ?
            ) AS allFiles
            ORDER BY shielded, isDone, fileId ASC
        """, (rs, row) -> new DowngradeFileRow(
                rs.getLong("fileId"),
                rs.getString("fileUrl"),
                rs.getLong("fileSize"),
                rs.getString("fileType")
        ), userId, userId);
    }

    public void deleteCreatorFile(long fileId) {
        jdbcTemplate.update("DELETE FROM TaskFiles WHERE id = ?", fileId);
    }

    public void deleteAssigneeFile(long fileId) {
        jdbcTemplate.update("DELETE FROM TaskAssigneeFiles WHERE id = ?", fileId);
    }

    // Clears grace-period fields once cleanup finishes or the deadline expires.
    public int clearGraceFields(long userId) {
        return jdbcTemplate.update("""
            UPDATE Users
            SET downgradeGraceDeadline = NULL,
                previousPlan           = NULL
            WHERE id = ?
        """, userId);
    }

    // Returns the user's current storage usage in bytes.
    // Used by maintenance to figure out whether they're over budget.
    public long getUsedStorageBytes(long userId) {
        Long used = jdbcTemplate.queryForObject(
                "SELECT usedStorageBytes FROM Users WHERE id = ?", Long.class, userId);
        return used != null ? used : 0L;
    }

    public record DowngradeFileRow(long fileId, String fileUrl, long fileSize, String fileType) {}
}
