package io.github.balasis.taskmanager.maintenance.service;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import io.github.balasis.taskmanager.maintenance.base.BaseComponent;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Scans the {@code assets} Azure Blob container and removes any
 * {@code .js} or {@code .css} blob that is older than 7 days.
 *
 * <p>Vite produces content-hashed bundles (e.g. {@code index-Bk2jQ.js}).
 * After a new frontend deploy the old hashes are no longer referenced, so
 * blobs older than the Front Door cache TTL (7 days) are safe to delete.
 *
 * <p>The {@link BlobServiceClient} already authenticates via Managed Identity
 * in production and via Azurite connection string in dev — no SAS required.
 */
@Service
@AllArgsConstructor
public class AssetCleanerService extends BaseComponent {

    private static final String ASSETS_CONTAINER = "assets";
    private static final Duration MAX_AGE = Duration.ofDays(7);

    private final BlobServiceClient blobServiceClient;

    /**
     * Iterates the blob listing lazily — only one page (~5 000 items) is in
     * memory at a time, so this stays safe even with very large containers.
     */
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
                continue; // leave other file types alone
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
