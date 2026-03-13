package io.github.balasis.taskmanager.engine.monitoring.health;

import com.azure.storage.blob.BlobServiceClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

// Health indicator for Blob Storage - lists containers to verify credentials and network.
@Component
public class BlobStorageHealthIndicator implements HealthIndicator {

    private final BlobServiceClient blobServiceClient;

    public BlobStorageHealthIndicator(BlobServiceClient blobServiceClient) {
        this.blobServiceClient = blobServiceClient;
    }

    @Override
    public Health health() {
        try {
            blobServiceClient.listBlobContainers().iterator().hasNext();
            return Health.up().build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
