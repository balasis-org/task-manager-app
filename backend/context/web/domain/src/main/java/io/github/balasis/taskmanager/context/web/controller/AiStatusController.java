package io.github.balasis.taskmanager.context.web.controller;

import io.github.balasis.taskmanager.engine.core.service.AiServiceHealthTracker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiStatusController {

    private final AiServiceHealthTracker tracker;

    public AiStatusController(AiServiceHealthTracker tracker) {
        this.tracker = tracker;
    }

    record AiStatus(boolean contentSafetyDegraded, boolean textAnalyticsDegraded) {}

    @GetMapping("/ai-status")
    public AiStatus getStatus() {
        return new AiStatus(
                tracker.isContentSafetyDegraded(),
                tracker.isTextAnalyticsDegraded()
        );
    }
}
