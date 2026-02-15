package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.TaskParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface TaskParticipantRepository extends JpaRepository<TaskParticipant, Long> {

    @Modifying
    @Query("delete from TaskParticipant tp where tp.user.id = :userId and tp.task.group.id = :groupId")
    void deleteByUserIdAndGroupId(@Param("userId") Long userId, @Param("groupId") Long groupId);

    @Modifying
    @Query("""
        delete from TaskParticipant tp
        where tp.user.id = :userId
          and tp.task.group.id = :groupId
          and tp.taskParticipantRole = io.github.balasis.taskmanager.context.base.enumeration.TaskParticipantRole.REVIEWER
    """)
    void deleteReviewersByUserIdAndGroupId(@Param("userId") Long userId, @Param("groupId") Long groupId);

    TaskParticipant findAllByTask_idAndUser_id(Long taskId,Long userId);
}
