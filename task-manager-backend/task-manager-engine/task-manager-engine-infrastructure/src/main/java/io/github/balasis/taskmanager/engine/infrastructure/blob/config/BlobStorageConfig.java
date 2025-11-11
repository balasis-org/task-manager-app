package io.github.balasis.taskmanager.engine.infrastructure.blob.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class BlobStorageConfig {
    private final SecretClientProvider secretClientProvider;

    @Bean
    public BlobServiceClient blobServiceClient() {
        String cs = secretClientProvider.getSecret("AZURITE-STORAGE-CONNECTION-STRING");
        if (cs != null) {
            return new BlobServiceClientBuilder().connectionString(cs).buildClient();
        }
        return new BlobServiceClientBuilder()
                .endpoint(secretClientProvider.getSecret("BLOB-SERVICE-ENDPOINT"))
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }
}

