package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByAzureKey(String azureKey);

    Optional<User> findByEmail(String email);

    boolean existsByAzureKey(String azureKey);

    Optional<User> findByInviteCode(String inviteCode);

    /**
     * Atomically increments the leader's storage counter.
     * Returns 0 (= no rows updated) when the increment would exceed the
     * given budget, so the caller can reject the upload cleanly.
     */
    @Modifying
    @Query("""
        UPDATE User u
        SET u.usedStorageBytes = u.usedStorageBytes + :size
        WHERE u.id = :userId
          AND u.usedStorageBytes + :size <= :budget
    """)
    int addStorageUsage(@Param("userId") Long userId,
                        @Param("size") long size,
                        @Param("budget") long budget);

    /**
     * Atomically decrements storage on file deletion.
     * Clamps to zero so that maintenance reconciliation is the only thing
     * that needs to handle negative drift.
     */
    @Modifying
    @Query("""
        UPDATE User u
        SET u.usedStorageBytes = CASE
            WHEN u.usedStorageBytes >= :size THEN u.usedStorageBytes - :size
            ELSE 0 END
        WHERE u.id = :userId
    """)
    void subtractStorageUsage(@Param("userId") Long userId,
                              @Param("size") long size);

    /**
     * Atomically increments the owner's monthly download counter.
     * Returns 0 when the increment would exceed the given budget.
     */
    @Modifying
    @Query("""
        UPDATE User u
        SET u.usedDownloadBytesMonth = u.usedDownloadBytesMonth + :size
        WHERE u.id = :userId
          AND u.usedDownloadBytesMonth + :size <= :budget
    """)
    int addDownloadUsage(@Param("userId") Long userId,
                         @Param("size") long size,
                         @Param("budget") long budget);

    /**
     * Atomically increments the owner's monthly email counter.
     * Returns 0 (no rows updated) when the quota would be exceeded,
     * so the caller silently skips the email.
     */
    @Modifying
    @Query("""
        UPDATE User u
        SET u.usedEmailsMonth = COALESCE(u.usedEmailsMonth, 0) + 1
        WHERE u.id = :userId
          AND COALESCE(u.usedEmailsMonth, 0) < :quota
    """)
    int incrementEmailUsage(@Param("userId") Long userId,
                            @Param("quota") int quota);

    @Modifying
    @Query("""
        UPDATE User u
        SET u.usedImageScansMonth = COALESCE(u.usedImageScansMonth, 0) + 1
        WHERE u.id = :userId
          AND COALESCE(u.usedImageScansMonth, 0) < :maxScans
    """)
    int incrementImageScanUsage(@Param("userId") Long userId,
                                @Param("maxScans") int maxScans);

    @Query("""
    select u
    from User u
    where (:q IS NULL OR u.name like concat('%',:q,'%')
              OR u.email like concat('%',:q,'%'))
    """)
    Page<User> searchUser(@Param("q") String q, Pageable pageable);

    @Query("""
    select u
    from User u
    where (:q IS NULL OR u.name like concat('%',:q,'%')
              OR u.email like concat('%',:q,'%'))
      and not exists (
          select gm.id from GroupMembership gm where gm.user = u and gm.group.id = :groupId
      )
      and (:tenantId IS NULL OR u.tenantId = :tenantId)
    """)
    Page<User> searchUserForInvites(@Param("groupId") Long groupId, @Param("q") String q, @Param("tenantId") String tenantId, Pageable pageable);
}
