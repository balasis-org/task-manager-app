package io.github.balasis.taskmanager.maintenance.blobcleaner.config;

import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"prod-h2","prod-azuresql"})
public class ManagedIdentityConfig {

    @Bean
    public ManagedIdentityCredential managedIdentityCredential() {
        return new ManagedIdentityCredentialBuilder()
                .clientId(System.getenv("MANAGED_IDENTITY_CLIENT_ID"))
                .build();
    }
}