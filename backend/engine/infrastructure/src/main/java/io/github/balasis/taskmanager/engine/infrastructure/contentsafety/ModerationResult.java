package io.github.balasis.taskmanager.engine.infrastructure.contentsafety;

// result from Azure Content Safety image analysis.
// safe() = image passed all 4 category checks (SEXUAL, VIOLENCE, HATE, SELF_HARM).
// rejected(cat, severity) = at least one category exceeded its severity threshold.
// severity is an integer 0-6: 0=safe, 2=low, 4=medium, 6=high.
// see ContentSafetyServiceImpl for the per-category thresholds.
public record ModerationResult(boolean isSafe, String rejectedCategory, Integer rejectedSeverity) {

    public static ModerationResult safe() {
        return new ModerationResult(true, null, null);
    }

    public static ModerationResult rejected(String category, int severity) {
        return new ModerationResult(false, category, severity);
    }
}
