package io.github.balasis.taskmanager.engine.infrastructure.secret.azure;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// creates the SecretClient bean pointing at the Key Vault URL from env.
// KEYVAULT_URI is set by the Bicep deployment, not stored in Key Vault itself.
@Profile({"prod-h2", "prod-azuresql", "prod-arena-stress", "prod-arena-security"})
@Configuration
@AllArgsConstructor
public class KeyVaultConfig {
    private final ManagedIdentityCredential managedIdentityCredential;

    @Bean
    public SecretClient secretClient() {

        return new SecretClientBuilder()
                .vaultUrl(System.getenv("KEYVAULT_URI"))
                .credential(managedIdentityCredential)
                .buildClient();
    }
}
