package io.github.balasis.taskmanager.engine.infrastructure.contentsafety.config;

import com.azure.ai.contentsafety.ContentSafetyClient;
import com.azure.ai.contentsafety.ContentSafetyClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.ContentSafetyService;
import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.service.ContentSafetyServiceImpl;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev-mssql", "dev-h2", "dev-flyway-mssql"})
@RequiredArgsConstructor
public class ContentSafetyDevConfig {
private final SecretClientProvider secretClientProvider;

    @Bean
    public ContentSafetyService contentSafetyService() {
        String endpoint = secretClientProvider.getSecret("TASKMANAGER-CONTENT-SAFETY-ENDPOINT");
        String key =secretClientProvider.getSecret("TASKMANAGER-CONTENT-SAFETY-KEY");

        ContentSafetyClient client = new ContentSafetyClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(key))
                .buildClient();

        return new ContentSafetyServiceImpl(client);
    }
}
