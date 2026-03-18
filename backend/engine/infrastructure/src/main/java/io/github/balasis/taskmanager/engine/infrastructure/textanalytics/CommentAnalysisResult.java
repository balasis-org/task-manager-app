package io.github.balasis.taskmanager.engine.infrastructure.textanalytics;

import java.util.List;

// per-comment result from Azure Text Analytics: sentiment label, confidence score,
// extracted key phrases, and how many PII entities were found
public record CommentAnalysisResult(
        String commentId,
        String sentiment,
        double confidence,
        List<String> keyPhrases,
        int piiEntityCount) {}
