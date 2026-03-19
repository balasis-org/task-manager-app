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

// prod: authenticates to Azure Content Safety via Managed Identity (no API key).
// Managed Identity: the App Service has an Azure AD service principal that the SDK
// uses automatically to get OAuth2 tokens for the Content Safety API.
// endpoint URL comes from Key Vault.
@Configuration
@Profile({"prod-h2", "prod-azuresql", "prod-arena-security"})
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
