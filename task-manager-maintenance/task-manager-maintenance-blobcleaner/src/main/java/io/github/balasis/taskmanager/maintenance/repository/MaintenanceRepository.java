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

    private final JdbcTemplate jdbcTemplate;

    public boolean existsById(BlobContainerType type, long id) {
        String sql = String.format(
                "SELECT COUNT(*) FROM %s WHERE id = ?",
                type.getTableName()
        );

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
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

    /**
     * Purge refresh tokens that expired more than 1 day ago.
     * Every login creates a new row; logout only deletes the current one,
     * so expired rows pile up for active users over time.
     */
    public int purgeExpiredRefreshTokens() {
        return jdbcTemplate.update("""
            DELETE FROM RefreshTokens
            WHERE expiresAt < DATEADD(DAY, -1, GETUTCDATE())
        """);
    }

    /**
     * Purge soft-delete tombstones older than 30 days.
     * The frontend only needs these for short-term sync;
     * keeping them forever wastes storage and slows queries.
     */
    public int purgeOldDeletedTasks(int retentionDays) {
        return jdbcTemplate.update("""
            DELETE FROM DeletedTasks
            WHERE deletedAt < DATEADD(DAY, ?, GETUTCDATE())
        """, -retentionDays);
    }

    /**
     * Purge accepted/declined invitations older than 30 days.
     * Only PENDING invitations need to stay — resolved ones are
     * never queried again but accumulate indefinitely.
     */
    public int purgeResolvedInvitations(int retentionDays) {
        return jdbcTemplate.update("""
            DELETE FROM GroupInvitations
            WHERE invitationStatus <> 'PENDING'
              AND createdAt < DATEADD(DAY, ?, GETUTCDATE())
        """, -retentionDays);
    }
}
