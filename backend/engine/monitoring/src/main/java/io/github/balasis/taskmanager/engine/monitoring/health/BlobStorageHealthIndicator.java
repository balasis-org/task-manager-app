package io.github.balasis.taskmanager.engine.monitoring.health;

import com.azure.storage.blob.BlobServiceClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

// Spring Boot Actuator health indicator for Azure Blob Storage.
// listBlobContainers() is a lightweight API call that verifies credentials,
// network connectivity, and that the storage account is reachable.
// shows up at /actuator/health as "blobStorage": {"status": "UP"} or "DOWN".
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
