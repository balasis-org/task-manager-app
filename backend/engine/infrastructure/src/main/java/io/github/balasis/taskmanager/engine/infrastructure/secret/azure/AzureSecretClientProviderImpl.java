package io.github.balasis.taskmanager.engine.infrastructure.secret.azure;

import com.azure.security.keyvault.secrets.SecretClient;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// prod: fetches secrets from Azure Key Vault on every call (not cached here,
// the Azure SDK has its own internal cache strategy)
@Profile({"prod-h2", "prod-azuresql", "prod-arena-stress", "prod-arena-security"})
@Component
public class AzureSecretClientProviderImpl implements SecretClientProvider {
    private final SecretClient secretClient;

    public AzureSecretClientProviderImpl(SecretClient secretClient){
        this.secretClient= secretClient;
    }

    @Override
    public String getSecret(String secretVar) {
        return this.secretClient.getSecret(secretVar).getValue();
    }

}
