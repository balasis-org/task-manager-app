package io.github.balasis.taskmanager.maintenance.blobcleaner.service;

import com.azure.storage.blob.models.BlobItem;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.maintenance.blobcleaner.base.BaseComponent;
import io.github.balasis.taskmanager.maintenance.blobcleaner.repository.BlobCleanerRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@AllArgsConstructor
public class BlobCleanerService extends BaseComponent {

    private final BlobCleanerRepository repository;
    private final BlobAccessService blobAccessService;

    public void clean(BlobContainerType type) {
        int batchSize = 50;
        long delayMillis = 200;

        Iterable<BlobItem> iterable = blobAccessService.listBlobs(type);
        List<BlobItem> allBlobs = new ArrayList<>();
        iterable.forEach(allBlobs::add);

       logger.trace("Scanning {} blobs in container {}", allBlobs.size(), type.getContainerName());

        for (int i = 0; i < allBlobs.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allBlobs.size());
            List<BlobItem> batch = allBlobs.subList(i, end);

            for (BlobItem blob : batch) {
                long id = extractId(blob.getName());
                if (id <= 0 || !repository.existsById(type, id)) {
                    blobAccessService.deleteBlob(type, blob.getName());
                    logger.trace("Deleted orphan blob: {}" , blob.getName());
                }
            }

            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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

