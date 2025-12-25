package io.github.balasis.taskmanager.maintenance.blobcleaner.config.secret.azure;

import com.azure.identity.ManagedIdentityCredential;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile({"prod-h2","prod-azuresql"})
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
