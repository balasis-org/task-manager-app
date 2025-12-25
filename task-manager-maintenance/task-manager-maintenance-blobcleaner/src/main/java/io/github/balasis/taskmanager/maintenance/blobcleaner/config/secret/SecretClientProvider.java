package io.github.balasis.taskmanager.maintenance.blobcleaner.config.secret;

public interface SecretClientProvider {
    String getSecret(String secretVar);
}
