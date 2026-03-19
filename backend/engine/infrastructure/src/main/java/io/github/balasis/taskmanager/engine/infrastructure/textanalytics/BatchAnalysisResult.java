package io.github.balasis.taskmanager.engine.infrastructure.textanalytics;

import java.util.List;

// aggregated result from Azure AI Language across all comments in a task.
// overallSentiment: majority vote — POSITIVE if most comments are positive, etc.
// overallConfidence: average of the highest confidence score per comment (0.0-1.0).
// topKeyPhrases: top-20 most frequent noun phrases extracted by Azure.
// totalPiiCount: total PII (Personally Identifiable Information) entities found
//   across all comments (SSNs, emails, phone numbers, credit cards, etc.).
public record BatchAnalysisResult(
        String overallSentiment,
        double overallConfidence,
        int positiveCount,
        int neutralCount,
        int negativeCount,
        List<String> topKeyPhrases,
        int totalPiiCount,
        List<CommentAnalysisResult> commentResults) {}
