package io.github.balasis.taskmanager.engine.infrastructure.blob.config;

import com.azure.identity.ManagedIdentityCredential;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// prod: Azure Blob Storage authenticated via Managed Identity.
// Managed Identity = the App Service has an Azure AD identity assigned to it,
// so it can access Key Vault / Blob Storage / etc. without storing any secrets.
// the SDK exchanges the MI token automatically — no connection string needed.
@Configuration
@Profile({"prod-h2", "prod-azuresql", "prod-arena-stress", "prod-arena-security"})
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
