package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.TaskComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    long countByTask_Id(Long taskId);

    @EntityGraph(attributePaths = {"creator"})
    Page<TaskComment> findAllByTask_id(Long taskId, Pageable pageable);

    @EntityGraph(attributePaths = {"creator"})
    Optional<TaskComment> findWithCreatorById(Long id);

    @Query("""
        SELECT tc
        FROM TaskComment tc
        LEFT JOIN FETCH tc.task t
        LEFT JOIN FETCH t.group
        LEFT JOIN FETCH tc.creator
        WHERE tc.id = :id
    """)
    Optional<TaskComment> findByIdWithTaskAndCreator(@Param("id") Long id);

    @Modifying
    @Query("""
        UPDATE TaskComment tc
        SET tc.creator = null, tc.creatorNameSnapshot = :creatorName
        WHERE tc.creator.id = :userId
          AND tc.task.id IN (SELECT t.id FROM Task t WHERE t.group.id = :groupId)
    """)
    void detachCreatorFromGroupComments(@Param("userId") Long userId,
                                        @Param("groupId") Long groupId,
                                        @Param("creatorName") String creatorName);

    @Modifying
    @Query("""
        UPDATE TaskComment tc
        SET tc.creator = null, tc.creatorNameSnapshot = :creatorName
        WHERE tc.creator.id = :userId
    """)
    void detachCreatorFromAllComments(@Param("userId") Long userId,
                                      @Param("creatorName") String creatorName);

    @Query("""
        SELECT tc
        FROM TaskComment tc
        LEFT JOIN FETCH tc.task t
        LEFT JOIN FETCH t.group g
        LEFT JOIN FETCH tc.creator c
        WHERE (:taskId IS NULL OR t.id = :taskId)
          AND (:groupId IS NULL OR g.id = :groupId)
          AND (:creatorId IS NULL OR c.id = :creatorId)
    """)
    Page<TaskComment> adminFilterComments(@Param("taskId") Long taskId,
                                          @Param("groupId") Long groupId,
                                          @Param("creatorId") Long creatorId,
                                          Pageable pageable);

    @Modifying
    @Query("""
        DELETE FROM TaskComment tc
        WHERE tc.task.id = :taskId
          AND tc.createdAt < :before
    """)
    int deleteByTaskIdAndCreatedAtBefore(@Param("taskId") Long taskId,
                                         @Param("before") Instant before);

    @Query("""
        SELECT COUNT(tc), COALESCE(SUM(LENGTH(tc.comment)), 0)
        FROM TaskComment tc
        WHERE tc.task.id = :taskId
    """)
    Object[] countAndSumCharsByTaskId(@Param("taskId") Long taskId);
}
