package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.DeletedTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Set;

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
