package io.github.balasis.taskmanager.maintenance.config.blob;

import com.azure.identity.ManagedIdentityCredential;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import io.github.balasis.taskmanager.maintenance.config.secret.SecretClientProvider;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"prod-h2","prod-azuresql"})
@AllArgsConstructor
public class BlobStorageProdConfig {

    private final SecretClientProvider secretClientProvider;
    private final ManagedIdentityCredential managedIdentityCredential;

    @Bean
    public BlobServiceClient blobServiceClient() {
        return new BlobServiceClientBuilder()
                .endpoint(secretClientProvider.getSecret("TASKMANAGER-BLOB-SERVICE-ENDPOINT"))
                .credential(managedIdentityCredential)
                .buildClient();
    }
}