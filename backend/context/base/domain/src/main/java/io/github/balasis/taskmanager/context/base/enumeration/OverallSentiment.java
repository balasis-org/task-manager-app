package io.github.balasis.taskmanager.context.base.enumeration;

// the overall sentiment of a task's comment thread as determined by
// Azure Text Analytics. MIXED means comments are split between positive
// and negative. stored in TaskAnalysisSnapshot after analysis runs.
public enum OverallSentiment {
    POSITIVE,
    MIXED,
    NEGATIVE
}
