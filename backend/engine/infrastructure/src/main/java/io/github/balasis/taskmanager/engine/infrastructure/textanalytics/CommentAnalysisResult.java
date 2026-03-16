package io.github.balasis.taskmanager.engine.infrastructure.textanalytics;

import java.util.List;

public record CommentAnalysisResult(
        String commentId,
        String sentiment,
        double confidence,
        List<String> keyPhrases,
        int piiEntityCount) {}
