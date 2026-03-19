package io.github.balasis.taskmanager.engine.infrastructure.contentsafety.config;

import com.azure.ai.contentsafety.ContentSafetyClient;
import com.azure.ai.contentsafety.ContentSafetyClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.ai.contentsafety.models.AnalyzeTextOptions;
import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.ContentSafetyService;
import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.ModerationResult;
import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.service.ContentSafetyServiceImpl;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
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

// dev: uses API key auth for Azure Content Safety (Managed Identity isn't available locally).
// if the env vars are missing or the key is invalid, gracefully falls back to no-op
// (all images pass moderation) so local devs can run the app without an Azure subscription.
// the probe call (analyzeText) forces eager credential validation at startup — the Azure SDK
// normally validates lazily on first real call, which would give confusing errors later.
// prints a big ASCII banner at startup when disabled so you cant miss it.
@Configuration
@Profile({"dev-mssql", "dev-h2", "dev-flyway-mssql"})
@RequiredArgsConstructor
public class ContentSafetyDevConfig {

    private static final Logger log = LoggerFactory.getLogger(ContentSafetyDevConfig.class);

    private final SecretClientProvider secretClientProvider;

    private boolean contentSafetyDisabled = false;

    // azure SDK validates credentials lazily so we probe it now to fail fast
    @Bean
    public ContentSafetyService contentSafetyService() {
        try {
            String endpoint = secretClientProvider.getSecret("TASKMANAGER-CONTENT-SAFETY-ENDPOINT");
            String key = secretClientProvider.getSecret("TASKMANAGER-CONTENT-SAFETY-KEY");

            if (endpoint == null || endpoint.isBlank() || key == null || key.isBlank()) {
                log.warn("Content Safety env vars are missing — falling back to no-op (all images allowed)");
                contentSafetyDisabled = true;
                return input -> ModerationResult.safe();
            }

            ContentSafetyClient client = new ContentSafetyClientBuilder()
                    .endpoint(endpoint)
                    .credential(new AzureKeyCredential(key))
                    .buildClient();

            // Azure SDK validates credentials lazily — force it now so the catch block handles 401s at startup
            client.analyzeText(new AnalyzeTextOptions("probe"));

            return new ContentSafetyServiceImpl(client);
        } catch (Exception e) {
            log.warn("Content Safety client failed to initialise: {} — falling back to no-op (all images allowed)",
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            contentSafetyDisabled = true;
            return input -> ModerationResult.safe();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void printContentSafetyBanner() {
        if (contentSafetyDisabled) {
            log.warn("\n\n"
                    + "╔══════════════════════════════════════════════════════════════╗\n"
                    + "║  CONTENT SAFETY IS DISABLED — all image uploads are allowed  ║\n"
                    + "║  Set TASKMANAGER-CONTENT-SAFETY-ENDPOINT and                 ║\n"
                    + "║  TASKMANAGER-CONTENT-SAFETY-KEY env vars to enable it.       ║\n"
                    + "╚══════════════════════════════════════════════════════════════╝\n");
        }
    }
}
