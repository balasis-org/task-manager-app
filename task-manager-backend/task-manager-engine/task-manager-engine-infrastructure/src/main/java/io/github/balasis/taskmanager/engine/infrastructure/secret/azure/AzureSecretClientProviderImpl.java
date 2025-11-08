package io.github.balasis.taskmanager.engine.infrastructure.secret.azure;

import com.azure.security.keyvault.secrets.SecretClient;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;


@Profile({"prod-h2","prod-azuresql"})
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
