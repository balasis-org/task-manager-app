package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.DeletedTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Set;

// polling tombstone table. when a task is deleted, a DeletedTask row is
// inserted so the frontend's next smart-poll can detect the deletion and
// remove it from the UI. entries are cleaned up by maintenance after 7 days.
@Repository
public interface DeletedTaskRepository extends JpaRepository<DeletedTask, Long> {

        @Query("""
                SELECT dt.deletedTaskId
                FROM DeletedTask dt
                WHERE dt.group.id = :groupId
                    AND dt.deletedAt > :since
        """)
        Set<Long> findDeletedTaskIdsByGroupIdAndDeletedAtAfter(@Param("groupId") Long groupId,
                                                                                                                        @Param("since") Instant since);

    void deleteAllByGroup_Id(Long groupId);
}
