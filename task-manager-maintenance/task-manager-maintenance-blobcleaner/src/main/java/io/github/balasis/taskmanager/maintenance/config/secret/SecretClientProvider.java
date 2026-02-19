package io.github.balasis.taskmanager.maintenance.config.secret;

public interface SecretClientProvider {
    String getSecret(String secretVar);
}
