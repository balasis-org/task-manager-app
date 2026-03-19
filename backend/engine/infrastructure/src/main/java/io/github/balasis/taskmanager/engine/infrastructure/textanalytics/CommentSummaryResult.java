package io.github.balasis.taskmanager.engine.infrastructure.textanalytics;

// extractive summary output: the concatenated top sentences + how many comments were fed in
public record CommentSummaryResult(String summaryText, int commentCount) {}
