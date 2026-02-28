package io.github.balasis.taskmanager.engine.monitoring.health;

import com.azure.storage.blob.BlobServiceClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reports Azure Blob Storage connectivity in /actuator/health.
 *
 * Lists containers (max 1 page) — a lightweight metadata request that verifies
 * credentials and network reach without touching any blob data.
 * Failure results in DOWN and triggers Azure health-probe restart.
 */
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
