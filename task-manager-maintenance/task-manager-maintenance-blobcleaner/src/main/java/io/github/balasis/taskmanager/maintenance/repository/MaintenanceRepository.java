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

    /* ── Blob cleanup ── */

    public boolean existsById(BlobContainerType type, long id) {
        String sql = String.format(
                "SELECT COUNT(*) FROM %s WHERE id = ?",
                type.getTableName()
        );

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    /* ── User cleanup ── */

    /**
     * Returns IDs of users who have no group memberships at all AND whose
     * last_active_at is older than 7 days (no profile image) or 14 days
     * (has a profile image).
     */
    public List<Long> findInactiveUserIdsWithoutGroups() {
        return jdbcTemplate.queryForList("""
            SELECT u.id
            FROM Users u
            WHERE NOT EXISTS (
                SELECT 1 FROM GroupMemberships gm WHERE gm.user_id = u.id
            )
            AND (u.systemRole IS NULL OR u.systemRole <> 'ADMIN')
            AND (
                (u.imgUrl IS NULL     AND u.lastActiveAt < DATEADD(DAY, -7,  GETUTCDATE()))
                OR
                (u.imgUrl IS NOT NULL AND u.lastActiveAt < DATEADD(DAY, -14, GETUTCDATE()))
            )
        """, Long.class);
    }

    /**
     * Attempts to delete a user who is not a member of any group.
     * <p>
     * Dependencies that are <b>already</b> cleaned by the main app when
     * a user leaves a group (TaskParticipants, TaskComment creator FK) do
     * not need handling here.  We only clean app-level artefacts that
     * survive independently of group membership (invitations, tokens).
     * <p>
     * If the final {@code DELETE FROM Users} fails (e.g. because the user
     * logged back in and joined a group between the check and the delete),
     * the exception is caught and the user is simply skipped — no row
     * locking required.
     *
     * @return {@code true} if the user was deleted, {@code false} if
     *         skipped due to a DB constraint (race condition / sync issue).
     */
    public boolean tryDeleteUser(Long userId) {
        try {
            //  app-level artefacts not tied to group-leave
            jdbcTemplate.update("DELETE FROM GroupInvitations WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM GroupInvitations WHERE invited_by_id = ?", userId);
            jdbcTemplate.update("DELETE FROM RefreshTokens    WHERE user_id = ?", userId);

            //  final delete — FK violation here means the user re-activated
            jdbcTemplate.update("DELETE FROM Users WHERE id = ?", userId);
            return true;
        } catch (DataAccessException e) {
            // FK violation or other constraint , skip da user
            return false;
        }
    }
}

