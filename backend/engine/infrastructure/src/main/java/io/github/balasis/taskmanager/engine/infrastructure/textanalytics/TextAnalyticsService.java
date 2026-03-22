package io.github.balasis.taskmanager.engine.infrastructure.textanalytics;

import java.util.List;

// infrastructure interface for Azure Text Analytics.
// analyzeBatch = sentiment + key phrases + PII in one API call.
// summarizeBatch = abstractive summarization (generates condensed new text).
public interface TextAnalyticsService {

    BatchAnalysisResult analyzeBatch(List<CommentDocument> comments);

    CommentSummaryResult summarizeBatch(List<CommentDocument> comments);
}
