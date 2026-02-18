package io.github.balasis.taskmanager.maintenance.config.blob;


import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import io.github.balasis.taskmanager.maintenance.config.secret.SecretClientProvider;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev-mssql", "dev-h2"})
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
