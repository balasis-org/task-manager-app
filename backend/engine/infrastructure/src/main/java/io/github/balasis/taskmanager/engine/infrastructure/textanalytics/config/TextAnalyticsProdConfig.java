package io.github.balasis.taskmanager.engine.infrastructure.textanalytics.config;

import com.azure.ai.textanalytics.TextAnalyticsClient;
import com.azure.ai.textanalytics.TextAnalyticsClientBuilder;
import com.azure.identity.ManagedIdentityCredential;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.TextAnalyticsService;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.service.TextAnalyticsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// prod: builds the Azure AI Language TextAnalyticsClient using Managed Identity.
// Managed Identity = the App Service gets an Azure AD token automatically,
// so no API key is stored anywhere. the endpoint URL comes from Key Vault.
// the TextAnalyticsClient is the main SDK entry point for all NLP operations
// (sentiment, key phrases, PII detection, extractive summary).
@Configuration
@Profile({"prod-h2", "prod-azuresql", "prod-arena-security"})
@RequiredArgsConstructor
public class TextAnalyticsProdConfig {

    private final SecretClientProvider secretClientProvider;
    private final ManagedIdentityCredential managedIdentityCredential;

    @Bean
    public TextAnalyticsService textAnalyticsService() {
        TextAnalyticsClient client = new TextAnalyticsClientBuilder()
                .endpoint(secretClientProvider.getSecret("TASKMANAGER-TEXT-ANALYTICS-ENDPOINT"))
                .credential(managedIdentityCredential)
                .buildClient();

        return new TextAnalyticsServiceImpl(client);
    }
}
