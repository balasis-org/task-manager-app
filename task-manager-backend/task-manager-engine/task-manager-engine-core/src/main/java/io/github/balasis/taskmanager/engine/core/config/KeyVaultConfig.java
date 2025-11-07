package io.github.balasis.taskmanager.engine.core.config;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("prod-azuresql")
@Configuration
public class KeyVaultConfig {

    @Bean
    public SecretClient secretClient() {
        String keyVaultUri = System.getenv("KEYVAULT_URI");
        String managedIdentityClientId = System.getenv("MANAGED_IDENTITY_CLIENT_ID");

        TokenCredential credential = new ManagedIdentityCredentialBuilder()
                .clientId(managedIdentityClientId)
                .build();

        return new SecretClientBuilder()
                .vaultUrl(keyVaultUri)
                .credential(credential)
                .buildClient();
    }
}
