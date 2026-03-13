package io.github.balasis.taskmanager.engine.infrastructure.secret.local;

import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile({"dev-h2","dev-mssql","dev-flyway-mssql"})
@Component
public class LocalSecretClientProviderImpl implements SecretClientProvider {

    @Override
    public String getSecret(String secretVar) {
        return System.getenv(secretVar);
    }
}
