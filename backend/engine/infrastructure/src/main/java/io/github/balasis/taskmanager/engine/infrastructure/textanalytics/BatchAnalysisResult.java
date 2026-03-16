package io.github.balasis.taskmanager.engine.infrastructure.textanalytics;

import java.util.List;

public record BatchAnalysisResult(
        String overallSentiment,
        double overallConfidence,
        int positiveCount,
        int neutralCount,
        int negativeCount,
        List<String> topKeyPhrases,
        int totalPiiCount,
        List<CommentAnalysisResult> commentResults) {}
