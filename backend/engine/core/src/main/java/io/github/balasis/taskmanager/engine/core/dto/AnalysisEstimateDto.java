package io.github.balasis.taskmanager.engine.core.dto;

import io.github.balasis.taskmanager.context.base.model.TaskAnalysisSnapshot;

public record AnalysisEstimateDto(TaskAnalysisSnapshot snapshot, int budgetUsed, int budgetMax) {
}
