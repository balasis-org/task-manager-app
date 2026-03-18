package io.github.balasis.taskmanager.engine.infrastructure.secret;

// thin abstraction: dev reads System.getenv(), prod reads Azure Key Vault.
public interface SecretClientProvider {
    String getSecret(String secretVar);
}
