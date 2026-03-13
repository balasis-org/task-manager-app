package io.github.balasis.taskmanager.engine.infrastructure.secret;

public interface SecretClientProvider {
    String getSecret(String secretVar);
}
