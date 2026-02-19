package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.TaskComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    long countByTask_Id(Long taskId);

    Page<TaskComment> findAllByTask_id(Long taskId, Pageable pageable);

    /**
     * Detaches the creator from all comments they wrote on tasks belonging
     * to the given group.  Sets creator to null and preserves the name
     * in creatorNameSnapshot so the UI can still display it.
     */
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
        LEFT JOIN tc.task t
        LEFT JOIN t.group g
        LEFT JOIN tc.creator c
        WHERE (:taskId IS NULL OR t.id = :taskId)
          AND (:groupId IS NULL OR g.id = :groupId)
          AND (:creatorId IS NULL OR c.id = :creatorId)
    """)
    Page<TaskComment> adminFilterComments(@Param("taskId") Long taskId,
                                          @Param("groupId") Long groupId,
                                          @Param("creatorId") Long creatorId,
                                          Pageable pageable);
}
