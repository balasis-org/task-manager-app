package io.github.balasis.taskmanager.context.base.enumeration;

// what kind of AI text analysis a user requested for a task's comments.
// ANALYSIS_ONLY: sentiment + key phrases + PII detection
// SUMMARY_ONLY: extractive summary of the conversation
// FULL: both analysis and summary in one request
// different types cost different amounts of credits.
public enum AnalysisType {
    ANALYSIS_ONLY,
    SUMMARY_ONLY,
    FULL
}
