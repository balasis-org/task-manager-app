package io.github.balasis.taskmanager.engine.infrastructure.blob.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// dev: uses Azurite — Microsoft's official Azure Storage emulator.
// Azurite runs as a Docker container (see backend-blob-compose.yml) and
// exposes the same REST API as real Azure Blob Storage on localhost:10000.
// the connection string is a well-known dev string (like SQL Server's sa password).
@Configuration
@Profile({"dev-mssql", "dev-h2", "dev-flyway-mssql"})
@AllArgsConstructor
public class BlobStorageDevConfig {

    private final SecretClientProvider secretClientProvider;

    @Bean
    public BlobServiceClient blobServiceClient() {
        String connectionString = secretClientProvider.getSecret("AZURITE-STORAGE-CONNECTION-STRING");
        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }
}
