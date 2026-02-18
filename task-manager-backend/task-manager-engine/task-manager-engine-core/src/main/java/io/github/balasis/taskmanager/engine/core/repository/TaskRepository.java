package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.base.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TaskRepository extends JpaRepository<Task,Long> {
    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, Long id);

    long countByGroup_Id(Long groupId);

    @Modifying
    void deleteAllByGroup_Id(Long groupId);

    @Query("""

    SELECT t
    FROM Task t
    LEFT JOIN FETCH t.group
    LEFT JOIN FETCH t.taskParticipants tp
    LEFT JOIN FETCH tp.user
    LEFT JOIN FETCH t.creatorFiles
    LEFT JOIN FETCH t.assigneeFiles
    WHERE t.id = :taskId
    """)
    Optional<Task> findByIdWithFullFetchParticipantsAndFiles(@Param("taskId") Long taskId);

    @Query("""

    SELECT t
    FROM Task t
    LEFT JOIN FETCH t.group
    LEFT JOIN FETCH t.taskParticipants tp
    LEFT JOIN FETCH t.creatorFiles
    LEFT JOIN FETCH t.assigneeFiles
    WHERE t.id = :taskId
    """)
    Optional<Task> findByIdWithParticipantsAndFiles(@Param("taskId") Long taskId);

    @Query("""
    SELECT t
    FROM Task t
    LEFT JOIN FETCH t.taskParticipants tp
    LEFT JOIN FETCH tp.user
    WHERE t.id=:taskId
    """)
    Optional<Task> findByIdWithTaskParticipants(@Param("taskId") Long taskId);

    @Query("""
    SELECT t
    FROM Task t
    LEFT JOIN FETCH t.creatorFiles
    LEFT JOIN FETCH t.assigneeFiles
    WHERE t.id = :taskId
""")
    Optional<Task> findByIdWithFiles(@Param("taskId") Long taskId);

    @Query("""
    SELECT t
    FROM Task t
    LEFT JOIN FETCH t.creatorFiles
    LEFT JOIN FETCH t.assigneeFiles
    LEFT JOIN FETCH t.group
    WHERE t.id = :taskId
""")
    Optional<Task> findByIdWithFilesAndGroup(@Param("taskId") Long taskId);

    @Query("""
    SELECT DISTINCT t
    FROM Task t
    LEFT JOIN FETCH t.taskParticipants tp
    LEFT JOIN FETCH tp.user
    WHERE t.group.id = :groupId
      AND (:taskState IS NULL OR t.taskState = :taskState)
      AND (
            (:reviewer IS TRUE AND t.id IN (
                SELECT t2.id
                FROM Task t2
                JOIN t2.taskParticipants tp2
                WHERE tp2.taskParticipantRole = 'REVIEWER' AND tp2.user.id = :userId
            ))
         OR (:assigned IS TRUE AND t.id IN (
                SELECT t2.id
                FROM Task t2
                JOIN t2.taskParticipants tp2
                WHERE tp2.taskParticipantRole = 'ASSIGNEE' AND tp2.user.id = :userId
            ))
         OR (:reviewer IS NULL AND :assigned IS NULL)
      )
""")
    Set<Task> searchBy(
           @Param("groupId") Long groupId,
           @Param("userId") Long userId,
           @Param("reviewer") Boolean reviewer,
           @Param("assigned") Boolean assigned,
           @Param("taskState") TaskState taskState
    );

        @Query("""
        SELECT DISTINCT t
        FROM Task t
        LEFT JOIN FETCH t.taskParticipants tp
        LEFT JOIN FETCH tp.user
        WHERE t.group.id = :groupId
        AND (:dueDateBefore IS NULL OR (t.dueDate IS NOT NULL AND t.dueDate <= :dueDateBefore))
        AND (:creatorId IS NULL OR t.id IN (
            SELECT t1.id
            FROM Task t1
            JOIN t1.taskParticipants tp1
            WHERE tp1.taskParticipantRole = 'CREATOR' AND tp1.user.id = :creatorId
        ))
        AND (:reviewerId IS NULL OR t.id IN (
            SELECT t2.id
            FROM Task t2
            JOIN t2.taskParticipants tp2
            WHERE tp2.taskParticipantRole = 'REVIEWER' AND tp2.user.id = :reviewerId
        ))
        AND (:assigneeId IS NULL OR t.id IN (
            SELECT t3.id
            FROM Task t3
            JOIN t3.taskParticipants tp3
            WHERE tp3.taskParticipantRole = 'ASSIGNEE' AND tp3.user.id = :assigneeId
        ))
        AND (:participantUserId IS NULL OR t.id IN (
            SELECT t4.id
            FROM Task t4
            JOIN t4.taskParticipants tp4
            WHERE tp4.user.id = :participantUserId
        ))
    """)
        Set<Task> searchTasksForPreviewWithFilters(
            @Param("groupId") Long groupId,
            @Param("creatorId") Long creatorId,
            @Param("reviewerId") Long reviewerId,
            @Param("assigneeId") Long assigneeId,
            @Param("participantUserId") Long participantUserId,
            @Param("dueDateBefore") Instant dueDateBefore
        );

    @Query("""
        SELECT DISTINCT t
        FROM Task t
        LEFT JOIN FETCH t.taskParticipants tp
        LEFT JOIN FETCH tp.user
        WHERE t.group.id = :groupId
          AND t.lastChangeDate > :since
    """)
    Set<Task> findChangedSince(
        @Param("groupId") Long groupId,
        @Param("since") Instant since
    );

    @Query("""
        SELECT DISTINCT t.id
        FROM Task t
        WHERE t.group.id = :groupId
        AND (:dueDateBefore IS NULL OR (t.dueDate IS NOT NULL AND t.dueDate <= :dueDateBefore))
        AND (:priorityMin IS NULL OR t.priority >= :priorityMin)
        AND (:priorityMax IS NULL OR t.priority <= :priorityMax)
        AND (:taskState IS NULL OR t.taskState = :taskState)
        AND (:hasFiles IS NULL
            OR (:hasFiles = TRUE AND (SIZE(t.creatorFiles) > 0 OR SIZE(t.assigneeFiles) > 0))
            OR (:hasFiles = FALSE AND SIZE(t.creatorFiles) = 0 AND SIZE(t.assigneeFiles) = 0)
        )
        AND (:creatorId IS NULL OR t.id IN (
            SELECT t1.id FROM Task t1 JOIN t1.taskParticipants tp1
            WHERE tp1.taskParticipantRole = 'CREATOR' AND tp1.user.id = :creatorId
        ))
        AND (:reviewerId IS NULL OR t.id IN (
            SELECT t2.id FROM Task t2 JOIN t2.taskParticipants tp2
            WHERE tp2.taskParticipantRole = 'REVIEWER' AND tp2.user.id = :reviewerId
        ))
        AND (:assigneeId IS NULL OR t.id IN (
            SELECT t3.id FROM Task t3 JOIN t3.taskParticipants tp3
            WHERE tp3.taskParticipantRole = 'ASSIGNEE' AND tp3.user.id = :assigneeId
        ))
        AND (:participantUserId IS NULL OR t.id IN (
            SELECT t4.id FROM Task t4 JOIN t4.taskParticipants tp4
            WHERE tp4.user.id = :participantUserId
        ))
    """)
    Set<Long> filterTaskIds(
            @Param("groupId") Long groupId,
            @Param("creatorId") Long creatorId,
            @Param("reviewerId") Long reviewerId,
            @Param("assigneeId") Long assigneeId,
            @Param("participantUserId") Long participantUserId,
            @Param("dueDateBefore") Instant dueDateBefore,
            @Param("priorityMin") Integer priorityMin,
            @Param("priorityMax") Integer priorityMax,
            @Param("taskState") TaskState taskState,
            @Param("hasFiles") Boolean hasFiles
    );

    @Modifying
    @Query("UPDATE Task t SET t.reviewedBy = null WHERE t.reviewedBy.id = :userId")
    void nullifyReviewedByForUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Task t SET t.lastEditBy = null WHERE t.lastEditBy.id = :userId")
    void nullifyLastEditByForUser(@Param("userId") Long userId);

}
