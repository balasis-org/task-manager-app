package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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
}
