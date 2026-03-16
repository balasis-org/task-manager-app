package io.github.balasis.taskmanager.engine.infrastructure.contentsafety;

public record ModerationResult(boolean isSafe, String rejectedCategory, Integer rejectedSeverity) {

    public static ModerationResult safe() {
        return new ModerationResult(true, null, null);
    }

    public static ModerationResult rejected(String category, int severity) {
        return new ModerationResult(false, category, severity);
    }
}
