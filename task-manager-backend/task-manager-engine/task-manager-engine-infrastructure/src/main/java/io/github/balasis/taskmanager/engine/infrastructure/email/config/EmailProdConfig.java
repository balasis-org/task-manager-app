package io.github.balasis.taskmanager.engine.infrastructure.email.config;

import com.azure.communication.email.EmailClientBuilder;
import com.azure.identity.ManagedIdentityCredential;
import io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient;
import io.github.balasis.taskmanager.engine.infrastructure.email.service.AzureEmailClient;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"prod-h2","prod-azuresql"})
@AllArgsConstructor
public class EmailProdConfig {
    private final ManagedIdentityCredential managedIdentityCredential;
    private final SecretClientProvider secretClientProvider;

    @Bean
    public EmailClient emailClient(){
        return new AzureEmailClient(
                new EmailClientBuilder()
                        .endpoint(secretClientProvider.getSecret("TASKMANAGER-ACS-ENDPOINT"))
                        .credential(managedIdentityCredential)
                        .buildClient(),secretClientProvider
        );
    }
}
