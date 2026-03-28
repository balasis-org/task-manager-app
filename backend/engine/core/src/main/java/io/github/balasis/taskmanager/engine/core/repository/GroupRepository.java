package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

// group queries are split between admin views (full eager fetches for the
// admin dashboard) and user views (lighter fetches). touchLastChangeByOwnerId
// bumps the group's timestamp so the smart-poll knows something changed.
@Repository
public interface GroupRepository extends JpaRepository<Group,Long> {
    boolean existsByNameAndOwner_Id(String name, Long ownerId);
    boolean existsByNameAndOwner_IdAndIdNot(String name, Long ownerId, Long id);
    long countByOwner_Id(Long ownerId);

    @Query("""
        SELECT DISTINCT g
        FROM Group g
        LEFT JOIN FETCH g.owner
        LEFT JOIN FETCH g.tasks t
        LEFT JOIN FETCH t.taskParticipants tp
        LEFT JOIN FETCH tp.user
        WHERE g.id = :groupId
    """)
    Optional<Group> findByIdWithTasksAndParticipants(@Param("groupId") Long groupId);

    @Query("""
        SELECT DISTINCT g
        FROM Group g
        LEFT JOIN FETCH g.owner
        LEFT JOIN FETCH g.tasks
        LEFT JOIN FETCH g.memberships m
        LEFT JOIN FETCH m.user
        WHERE g.id = :groupId
    """)
    Optional<Group> adminFindByIdWithDetails(@Param("groupId") Long groupId);

    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.owner WHERE g.id = :id")
    Optional<Group> findByIdWithOwner(@Param("id") Long id);

    @Query(value = """
        SELECT g
        FROM Group g
        LEFT JOIN FETCH g.owner o
        LEFT JOIN FETCH g.memberships
        LEFT JOIN FETCH g.tasks
        WHERE g.name LIKE concat('%', :q, '%')
           OR o.name LIKE concat('%', :q, '%')
    """, countQuery = """
        SELECT COUNT(g)
        FROM Group g
        LEFT JOIN g.owner o
        WHERE g.name LIKE concat('%', :q, '%')
           OR o.name LIKE concat('%', :q, '%')
    """)
    Page<Group> adminSearchGroups(@Param("q") String q, Pageable pageable);

    @Query(value = """
        SELECT g
        FROM Group g
        LEFT JOIN FETCH g.owner
        LEFT JOIN FETCH g.memberships
        LEFT JOIN FETCH g.tasks
    """, countQuery = "SELECT COUNT(g) FROM Group g")
    Page<Group> adminFindAllGroups(Pageable pageable);

    @Modifying
    @Query("UPDATE Group g SET g.lastChangeInGroup = :now WHERE g.owner.id = :ownerId")
    void touchLastChangeByOwnerId(@Param("ownerId") Long ownerId, @Param("now") Instant now);

    // Atomically bumps the group's member counter.
    // Returns 0 (no rows updated) when the group is already at its cap,
    // so the caller can reject the join.
    @Modifying
    @Query("""
        UPDATE Group g
        SET g.memberCount = g.memberCount + 1
        WHERE g.id = :groupId
          AND g.memberCount < :maxMembers
    """)
    int incrementMemberCount(@Param("groupId") Long groupId,
                             @Param("maxMembers") int maxMembers);

    // Atomically decrements member count on removal / leave.
    // Clamps to zero; maintenance reconciliation handles any drift.
    @Modifying
    @Query("""
        UPDATE Group g
        SET g.memberCount = CASE
            WHEN g.memberCount > 0 THEN g.memberCount - 1
            ELSE 0 END
        WHERE g.id = :groupId
    """)
    void decrementMemberCount(@Param("groupId") Long groupId);
}
