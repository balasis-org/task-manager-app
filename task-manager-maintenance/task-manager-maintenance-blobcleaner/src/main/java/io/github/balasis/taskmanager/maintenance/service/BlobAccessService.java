package io.github.balasis.taskmanager.maintenance.service;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BlobAccessService {

    private final BlobServiceClient blobServiceClient;

    public Iterable<BlobItem> listBlobs(BlobContainerType type) {
        return blobServiceClient
                .getBlobContainerClient(type.getContainerName())
                .listBlobs();
    }

    public void deleteBlob(BlobContainerType type, String blobName) {
        blobServiceClient
                .getBlobContainerClient(type.getContainerName())
                .getBlobClient(blobName)
                .delete();
    }
}

