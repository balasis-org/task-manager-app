package io.github.balasis.taskmanager.maintenance.service;

import com.azure.storage.blob.models.BlobItem;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.maintenance.base.BaseComponent;
import io.github.balasis.taskmanager.maintenance.repository.MaintenanceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class BlobCleanerService extends BaseComponent {

    private final MaintenanceRepository repository;
    private final BlobAccessService blobAccessService;

    public void clean(BlobContainerType type) {
        int batchSize = 50;
        long delayMillis = 200;

        Iterable<BlobItem> iterable = blobAccessService.listBlobs(type);

        logger.info("Starting orphan-blob scan in container '{}'", type.getContainerName());

        int scanned = 0;
        int deleted = 0;
        List<BlobItem> batch = new ArrayList<>(batchSize);

        for (BlobItem blob : iterable) {
            batch.add(blob);
            if (batch.size() >= batchSize) {
                deleted += processBatch(batch, type);
                scanned += batch.size();
                batch.clear();
                sleep(delayMillis);
            }
        }

        if (!batch.isEmpty()) {
            deleted += processBatch(batch, type);
            scanned += batch.size();
        }

        logger.info("Finished orphan-blob scan in '{}': scanned={}, deleted={}",
                type.getContainerName(), scanned, deleted);
    }

    private int processBatch(List<BlobItem> batch, BlobContainerType type) {
        int deleted = 0;
        for (BlobItem blob : batch) {
            long id = extractId(blob.getName());
            if (id <= 0 || !repository.existsById(type, id)) {
                blobAccessService.deleteBlob(type, blob.getName());
                logger.info("Deleted orphan blob: {}", blob.getName());
                deleted++;
            }
        }
        return deleted;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private long extractId(String blobName) {
        int dash = blobName.indexOf('-');
        if (dash <= 0) return -1;

        try {
            return Long.parseLong(blobName.substring(0, dash));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
