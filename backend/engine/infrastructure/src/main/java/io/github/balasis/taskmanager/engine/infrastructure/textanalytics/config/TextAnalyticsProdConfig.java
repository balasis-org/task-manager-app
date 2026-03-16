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
