package io.github.balasis.taskmanager.engine.infrastructure.email.config;

import com.azure.communication.email.EmailClientBuilder;
import com.azure.identity.ManagedIdentityCredential;
import io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient;
import io.github.balasis.taskmanager.engine.infrastructure.email.service.AzureEmailClient;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// prod: two separate Azure Communication Services email clients.
// userEmailClient sends to regular users (invitations, notifications).
// adminEmailClient sends from a different domain/sender for admin alerts.
// both use managed identity.
@Configuration
@Profile({"prod-h2","prod-azuresql"})
@AllArgsConstructor
public class EmailProdConfig {
    private final ManagedIdentityCredential managedIdentityCredential;
    private final SecretClientProvider secretClientProvider;

    @Bean
    @Qualifier("userEmailClient")
    public EmailClient userEmailClient(){
        String senderAddress = secretClientProvider.getSecret("TASKMANAGER-EMAIL-SENDER-ADDRESS");
        return new AzureEmailClient(
                new EmailClientBuilder()
                        .endpoint(secretClientProvider.getSecret("TASKMANAGER-ACS-ENDPOINT"))
                        .credential(managedIdentityCredential)
                        .buildClient(),
                senderAddress
        );
    }

    @Bean
    @Qualifier("adminEmailClient")
    public EmailClient adminEmailClient(){
        String senderAddress = secretClientProvider.getSecret("TASKMANAGER-EMAIL-ADMIN-SENDER-ADDRESS");
        return new AzureEmailClient(
                new EmailClientBuilder()
                        .endpoint(secretClientProvider.getSecret("TASKMANAGER-ACS-ADMIN-ENDPOINT"))
                        .credential(managedIdentityCredential)
                        .buildClient(),
                senderAddress
        );
    }
}
