package io.github.balasis.taskmanager.engine.infrastructure.contentsafety.config;

import com.azure.ai.contentsafety.ContentSafetyClient;
import com.azure.ai.contentsafety.ContentSafetyClientBuilder;
import com.azure.identity.ManagedIdentityCredential;
import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.ContentSafetyService;
import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.service.ContentSafetyServiceImpl;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"prod-h2","prod-azuresql"})
@RequiredArgsConstructor
public class ContentSafetyProdConfig {
    private final SecretClientProvider secretClientProvider;
    private final ManagedIdentityCredential managedIdentityCredential;

    @Bean
    public ContentSafetyService contentSafetyService() {
        ContentSafetyClient client = new ContentSafetyClientBuilder()
                .endpoint(secretClientProvider.getSecret("TASKMANAGER-CONTENT-SAFETY-ENDPOINT"))
                .credential(managedIdentityCredential)
                .buildClient();

        return new ContentSafetyServiceImpl(client);
    }
}
