package io.github.balasis.taskmanager.engine.infrastructure.textanalytics.config;

import com.azure.ai.textanalytics.TextAnalyticsClient;
import com.azure.ai.textanalytics.TextAnalyticsClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.BatchAnalysisResult;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.CommentAnalysisResult;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.CommentSummaryResult;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.TextAnalyticsService;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.service.TextAnalyticsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;

// dev: API key auth for Azure AI Language (Managed Identity isnt available locally).
// AzureKeyCredential wraps the API key for the SDK's Authorization header.
// no-op fallback: if env vars are missing, returns neutral sentiment for everything
// so the UI still renders normally. the probe call (detectLanguage) forces eager
// credential validation — the SDK otherwise validates lazily, leading to confusing errors.
// prints a big ASCII banner at startup when disabled.
@Configuration
@Profile({"dev-mssql", "dev-h2", "dev-flyway-mssql"})
@RequiredArgsConstructor
public class TextAnalyticsDevConfig {

    private static final Logger log = LoggerFactory.getLogger(TextAnalyticsDevConfig.class);

    private final SecretClientProvider secretClientProvider;

    private boolean textAnalyticsDisabled = false;

    @Bean
    public TextAnalyticsService textAnalyticsService() {
        try {
            String endpoint = secretClientProvider.getSecret("TASKMANAGER-TEXT-ANALYTICS-ENDPOINT");
            String key = secretClientProvider.getSecret("TASKMANAGER-TEXT-ANALYTICS-KEY");

            if (endpoint == null || endpoint.isBlank() || key == null || key.isBlank()) {
                log.warn("Text Analytics env vars are missing — falling back to no-op (neutral results)");
                textAnalyticsDisabled = true;
                return noOp();
            }

            TextAnalyticsClient client = new TextAnalyticsClientBuilder()
                    .endpoint(endpoint)
                    .credential(new AzureKeyCredential(key))
                    .buildClient();

            // Probe call to verify credentials at startup
            client.detectLanguage("probe");

            return new TextAnalyticsServiceImpl(client);
        } catch (Exception e) {
            log.warn("Text Analytics client failed to initialise: {} — falling back to no-op",
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            textAnalyticsDisabled = true;
            return noOp();
        }
    }

    private TextAnalyticsService noOp() {
        return new TextAnalyticsService() {
            @Override
            public BatchAnalysisResult analyzeBatch(List<io.github.balasis.taskmanager.engine.infrastructure.textanalytics.CommentDocument> comments) {
                List<CommentAnalysisResult> results = comments.stream()
                        .map(c -> new CommentAnalysisResult(c.id(), "neutral", 1.0, List.of(), 0))
                        .toList();
                return new BatchAnalysisResult("MIXED", 0.33, 0, comments.size(), 0, List.of(), 0, results);
            }

            @Override
            public CommentSummaryResult summarizeBatch(List<io.github.balasis.taskmanager.engine.infrastructure.textanalytics.CommentDocument> comments) {
                return new CommentSummaryResult("AI service unavailable in dev mode — no summary generated.", comments.size());
            }
        };
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void printTextAnalyticsBanner() {
        if (textAnalyticsDisabled) {
            log.warn("\n\n"
                    + "╔══════════════════════════════════════════════════════════════╗\n"
                    + "║  TEXT ANALYTICS IS DISABLED — analysis returns neutral data   ║\n"
                    + "║  Set TASKMANAGER-TEXT-ANALYTICS-ENDPOINT and                  ║\n"
                    + "║  TASKMANAGER-TEXT-ANALYTICS-KEY env vars to enable it.        ║\n"
                    + "╚══════════════════════════════════════════════════════════════╝\n");
        }
    }
}
