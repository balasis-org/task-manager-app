package io.github.balasis.taskmanager.maintenance.service;

import com.azure.storage.blob.models.BlobItem;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.maintenance.base.BaseComponent;
import io.github.balasis.taskmanager.maintenance.repository.MaintenanceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// scans a blob container for orphans (blobs not referenced by any DB row)
// and deletes them. processes in batches of 50 with 200ms pauses to stay
// gentle on Azure storage rate limits.
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
        // task-file blobs use the full blob name as FK, image blobs use
        // an "entityId-uuid" naming convention so we extract the numeric prefix
        boolean isTaskContainer = type == BlobContainerType.TASK_FILES
                || type == BlobContainerType.TASK_ASSIGNEE_FILES;

        for (BlobItem blob : batch) {
            boolean isOrphan;
            if (isTaskContainer) {
                isOrphan = !repository.existsByBlobName(type, blob.getName());
            } else {
                long id = extractId(blob.getName());
                isOrphan = id <= 0 || !repository.existsById(type, id);
            }

            if (isOrphan) {
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
