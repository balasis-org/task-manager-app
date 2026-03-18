package io.github.balasis.taskmanager.engine.infrastructure.textanalytics.config;

import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.BatchAnalysisResult;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.CommentAnalysisResult;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.CommentDocument;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.CommentSummaryResult;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.TextAnalyticsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

// arena stress: returns neutral data so load tests dont bill the AI APIs
@Configuration
@Profile("prod-arena-stress")
public class NoOpTextAnalyticsConfig {

    @Bean
    public TextAnalyticsService textAnalyticsService() {
        return new TextAnalyticsService() {
            @Override
            public BatchAnalysisResult analyzeBatch(List<CommentDocument> comments) {
                List<CommentAnalysisResult> results = comments.stream()
                        .map(c -> new CommentAnalysisResult(c.id(), "neutral", 1.0, List.of(), 0))
                        .toList();
                return new BatchAnalysisResult("MIXED", 0.33, 0, comments.size(), 0, List.of(), 0, results);
            }

            @Override
            public CommentSummaryResult summarizeBatch(List<CommentDocument> comments) {
                return new CommentSummaryResult("Stress profile — no AI summary.", comments.size());
            }
        };
    }
}
