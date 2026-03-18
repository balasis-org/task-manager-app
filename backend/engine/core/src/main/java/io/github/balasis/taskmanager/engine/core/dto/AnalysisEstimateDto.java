package io.github.balasis.taskmanager.engine.core.dto;

import io.github.balasis.taskmanager.context.base.model.TaskAnalysisSnapshot;

// returned by the "estimate analysis cost" endpoint before the user
// confirms spending credits on AI analysis
public record AnalysisEstimateDto(TaskAnalysisSnapshot snapshot, int budgetUsed, int budgetMax) {
}
