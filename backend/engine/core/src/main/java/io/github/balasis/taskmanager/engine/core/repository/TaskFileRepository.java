package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.TaskFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

// creator-uploaded files (task attachments set by leader/manager).
// the two query variants: one for leaders who see all files in the group,
// one for participants who only see files on tasks theyre involved in.
@Repository
public interface TaskFileRepository extends JpaRepository<TaskFile, Long> {

    @Query("SELECT f FROM TaskFile f LEFT JOIN FETCH f.uploadedBy JOIN FETCH f.task t "
         + "WHERE t.group.id = :groupId")
    List<TaskFile> findAllByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT f FROM TaskFile f LEFT JOIN FETCH f.uploadedBy JOIN FETCH f.task t "
         + "WHERE t.group.id = :groupId AND EXISTS "
         + "(SELECT tp FROM TaskParticipant tp WHERE tp.task = t AND tp.user.id = :userId)")
    List<TaskFile> findAllByGroupIdAndParticipant(@Param("groupId") Long groupId,
                                                  @Param("userId") Long userId);
}
