package io.github.balasis.taskmanager.engine.infrastructure.textanalytics;

import io.github.balasis.taskmanager.context.base.enumeration.AnalysisType;
import io.github.balasis.taskmanager.context.base.model.TaskAnalysisSnapshot;

// business-level analysis service (core layer implements this).
// estimate → hasActive check → enqueue → drainer processes → getSnapshot.
public interface TaskAnalysisService {

    TaskAnalysisSnapshot estimateCredits(Long taskId);

    boolean hasActiveRequest(Long taskId);

    void enqueueAnalysis(Long taskId, Long groupId, Long userId,
                         AnalysisType type, int credits);

    TaskAnalysisSnapshot getSnapshot(Long taskId);
}
