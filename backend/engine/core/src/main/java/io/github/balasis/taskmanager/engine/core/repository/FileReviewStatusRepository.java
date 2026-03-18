package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.FileReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

// per-file review decisions (APPROVED, REJECTED, NEEDS_CHANGES) by reviewers.
// both creator files and assignee files can be reviewed, hence the two
// parallel sets of queries. bulk deletes clean up when a group or task is removed.
@Repository
public interface FileReviewStatusRepository extends JpaRepository<FileReviewStatus, Long> {

    @Query("SELECT frs FROM FileReviewStatus frs LEFT JOIN FETCH frs.reviewer WHERE frs.taskFile.id IN :ids")
    List<FileReviewStatus> findByTaskFileIdIn(@Param("ids") Set<Long> ids);

    @Query("SELECT frs FROM FileReviewStatus frs LEFT JOIN FETCH frs.reviewer WHERE frs.taskAssigneeFile.id IN :ids")
    List<FileReviewStatus> findByTaskAssigneeFileIdIn(@Param("ids") Set<Long> ids);

    Optional<FileReviewStatus> findByTaskFile_IdAndReviewer_Id(Long taskFileId, Long reviewerId);

    Optional<FileReviewStatus> findByTaskAssigneeFile_IdAndReviewer_Id(Long taskAssigneeFileId, Long reviewerId);

    @Modifying
    @Query("DELETE FROM FileReviewStatus frs WHERE frs.taskFile.id = :fileId")
    void deleteByTaskFileId(@Param("fileId") Long fileId);

    @Modifying
    @Query("DELETE FROM FileReviewStatus frs WHERE frs.taskAssigneeFile.id = :fileId")
    void deleteByTaskAssigneeFileId(@Param("fileId") Long fileId);

    @Modifying
    @Query("DELETE FROM FileReviewStatus frs WHERE frs.taskFile.id IN " +
           "(SELECT tf.id FROM TaskFile tf WHERE tf.task.group.id = :groupId)")
    void deleteAllByTaskFileGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM FileReviewStatus frs WHERE frs.taskAssigneeFile.id IN " +
           "(SELECT af.id FROM TaskAssigneeFile af WHERE af.task.group.id = :groupId)")
    void deleteAllByTaskAssigneeFileGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM FileReviewStatus frs WHERE frs.taskFile.id IN " +
           "(SELECT tf.id FROM TaskFile tf WHERE tf.task.id = :taskId)")
    void deleteAllByTaskFileTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("DELETE FROM FileReviewStatus frs WHERE frs.taskAssigneeFile.id IN " +
           "(SELECT af.id FROM TaskAssigneeFile af WHERE af.task.id = :taskId)")
    void deleteAllByTaskAssigneeFileTaskId(@Param("taskId") Long taskId);
}
