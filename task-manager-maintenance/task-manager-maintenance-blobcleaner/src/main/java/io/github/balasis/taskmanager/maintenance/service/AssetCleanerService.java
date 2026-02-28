package io.github.balasis.taskmanager.maintenance.service;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import io.github.balasis.taskmanager.maintenance.base.BaseComponent;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@AllArgsConstructor
public class AssetCleanerService extends BaseComponent {

    private static final String ASSETS_CONTAINER = "assets";
    private static final Duration MAX_AGE = Duration.ofDays(7);

    private final BlobServiceClient blobServiceClient;

    public void cleanOldAssets() {
        Instant cutoff = Instant.now().minus(MAX_AGE);

        Iterable<BlobItem> blobs = blobServiceClient
                .getBlobContainerClient(ASSETS_CONTAINER)
                .listBlobs();

        logger.info("AssetCleaner: starting scan of container '{}'", ASSETS_CONTAINER);

        int deleted = 0;
        int skipped = 0;
        int scanned = 0;

        for (BlobItem blob : blobs) {
            scanned++;
            String name = blob.getName();
            if (!name.endsWith(".js") && !name.endsWith(".css")) {
                continue;
            }

            var lastModified = blob.getProperties() != null
                    ? blob.getProperties().getLastModified()
                    : null;

            if (lastModified == null) {
                skipped++;
                logger.warn("AssetCleaner: blob '{}' has no last-modified timestamp — skipping", name);
                continue;
            }

            if (lastModified.toInstant().isBefore(cutoff)) {
                try {
                    blobServiceClient
                            .getBlobContainerClient(ASSETS_CONTAINER)
                            .getBlobClient(name)
                            .delete();
                    deleted++;
                    logger.info("AssetCleaner: deleted old asset '{}'", name);
                } catch (Exception e) {
                    skipped++;
                    logger.error("AssetCleaner: failed to delete '{}': {}", name, e.getMessage());
                }
            }
        }

        logger.info("AssetCleaner: finished. scanned={}, deleted={}, skipped={}", scanned, deleted, skipped);
    }
}
