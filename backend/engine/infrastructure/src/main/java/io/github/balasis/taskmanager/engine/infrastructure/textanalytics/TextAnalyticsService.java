package io.github.balasis.taskmanager.engine.infrastructure.textanalytics;

import java.util.List;

public interface TextAnalyticsService {

    BatchAnalysisResult analyzeBatch(List<CommentDocument> comments);

    CommentSummaryResult summarizeBatch(List<CommentDocument> comments);
}
