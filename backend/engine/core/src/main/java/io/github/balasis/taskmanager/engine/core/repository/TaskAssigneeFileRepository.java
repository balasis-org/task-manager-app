package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.TaskAssigneeFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAssigneeFileRepository extends JpaRepository<TaskAssigneeFile, Long> {

    @Query("SELECT f FROM TaskAssigneeFile f LEFT JOIN FETCH f.uploadedBy JOIN FETCH f.task t "
         + "WHERE t.group.id = :groupId")
    List<TaskAssigneeFile> findAllByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT f FROM TaskAssigneeFile f LEFT JOIN FETCH f.uploadedBy JOIN FETCH f.task t "
         + "WHERE t.group.id = :groupId AND EXISTS "
         + "(SELECT tp FROM TaskParticipant tp WHERE tp.task = t AND tp.user.id = :userId)")
    List<TaskAssigneeFile> findAllByGroupIdAndParticipant(@Param("groupId") Long groupId,
                                                          @Param("userId") Long userId);
}
