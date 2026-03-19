package io.github.balasis.taskmanager.engine.infrastructure;

import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// single managed identity credential bean shared by Key Vault, Blob, Redis,
// Content Safety, Text Analytics, ACS email. MANAGED_IDENTITY_CLIENT_ID
// is set by the Bicep deployment (user-assigned identity).
@Configuration
@Profile({"prod-h2", "prod-azuresql", "prod-arena-stress", "prod-arena-security"})
public class ManagedIdentityConfig {

    @Bean
    public ManagedIdentityCredential managedIdentityCredential() {
        return new ManagedIdentityCredentialBuilder()
                .clientId(System.getenv("MANAGED_IDENTITY_CLIENT_ID"))
                .build();
    }
}
