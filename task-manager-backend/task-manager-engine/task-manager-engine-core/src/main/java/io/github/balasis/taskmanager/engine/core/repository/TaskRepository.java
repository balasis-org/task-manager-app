package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.base.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface TaskRepository extends JpaRepository<Task,Long> {
    @Modifying
    void deleteAllByGroup_Id(Long groupId);

    @Query("""

    SELECT t
    FROM Task t
    LEFT JOIN FETCH t.creator
    LEFT JOIN FETCH t.group
    LEFT JOIN FETCH t.taskParticipants tp
    LEFT JOIN FETCH tp.user
    LEFT JOIN FETCH t.files
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
    LEFT JOIN FETCH t.files
    WHERE t.id = :taskId
""")
    Optional<Task> findByIdWithFiles(@Param("taskId") Long taskId);

    @Query("""
    SELECT t
    FROM Task t
    LEFT JOIN FETCH t.files
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
}
