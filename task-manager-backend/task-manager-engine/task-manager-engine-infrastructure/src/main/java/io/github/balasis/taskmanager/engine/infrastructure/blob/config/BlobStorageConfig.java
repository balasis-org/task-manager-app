package io.github.balasis.taskmanager.engine.infrastructure.blob.config;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class BlobStorageConfig {
    private final SecretClientProvider secretClientProvider;
    private static final Logger logger = LoggerFactory.getLogger(BlobStorageConfig.class);


    @Bean
    public BlobServiceClient blobServiceClient() {
        String connectionString = null;
        try {
            connectionString = secretClientProvider.getSecret("AZURITE-STORAGE-CONNECTION-STRING");
        } catch (Exception e) {
            logger.info("No Azurite storage connection string found, using default endpoint...");
        }

        if (connectionString != null && !connectionString.isBlank()) {
            return new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
        }else{
            // Placeholder for managed identity credential
            var managedIdentityCredential =  new ManagedIdentityCredentialBuilder()
                    .clientId( secretClientProvider.getSecret("MANAGED_IDENTITY_CLIENT_ID"))
                    .build();

            return new BlobServiceClientBuilder()
                    .endpoint(secretClientProvider.getSecret("TASKMANAGER-BLOB-SERVICE-ENDPOINT"))
                    .credential(managedIdentityCredential)
                    .buildClient();
        }

    }
}

