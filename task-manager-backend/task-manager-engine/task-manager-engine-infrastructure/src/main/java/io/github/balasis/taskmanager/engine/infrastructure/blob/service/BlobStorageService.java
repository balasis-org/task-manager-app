package io.github.balasis.taskmanager.engine.infrastructure.blob.service;


import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobContainerClient;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class BlobStorageService {//TODO: change await to response time? to avoid delay of deletion if down?
    private final BlobContainerClient containerClient;

    public BlobStorageService(SecretClientProvider secretClientProvider, BlobServiceClient blobServiceClient) {
        String containerName = secretClientProvider.getSecret("TASKMANAGER-BLOB-CONTAINERS-NAME");
        this.containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient.create();
        }
    }

    public String upload(MultipartFile file) throws IOException {
        String blobName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.upload(file.getInputStream(), file.getSize(), true);
        String fullUrl = blobClient.getBlobUrl();
        return fullUrl.substring(fullUrl.indexOf(containerClient.getBlobContainerName())
                + containerClient.getBlobContainerName().length() + 1);
    }

    public byte[] download(String blobName) throws IOException {
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        if (!blobClient.exists()) {
            throw new RuntimeException("Blob not found: " + blobName);
        }
        try (var inputStream = blobClient.openInputStream()) {
            return inputStream.readAllBytes();
        }
    }

    public void delete(String blobName) {
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        if (blobClient.exists()) {
            blobClient.delete();
        }
    }
}
