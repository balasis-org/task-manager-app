package io.github.balasis.taskmanager.engine.core.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

// Tracks AI service degradation state (in-memory, per-instance).
// Set to degraded when an AI call fails; cleared on the next success.
// Read by AiStatusController to expose status to the frontend.
@Component
public class AiServiceHealthTracker {

    private final AtomicBoolean contentSafetyDegraded = new AtomicBoolean(false);
    private final AtomicBoolean textAnalyticsDegraded = new AtomicBoolean(false);

    public void markContentSafetyDegraded() { contentSafetyDegraded.set(true); }
    public void markContentSafetyHealthy()  { contentSafetyDegraded.set(false); }
    public boolean isContentSafetyDegraded() { return contentSafetyDegraded.get(); }

    public void markTextAnalyticsDegraded() { textAnalyticsDegraded.set(true); }
    public void markTextAnalyticsHealthy()  { textAnalyticsDegraded.set(false); }
    public boolean isTextAnalyticsDegraded() { return textAnalyticsDegraded.get(); }
}
