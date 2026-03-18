package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.TaskAnalysisSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// stores the latest AI analysis result per task. one snapshot per task —
// re-running analysis overwrites the existing snapshot.
public interface TaskAnalysisSnapshotRepository extends JpaRepository<TaskAnalysisSnapshot, Long> {

    Optional<TaskAnalysisSnapshot> findByTaskId(Long taskId);
}
