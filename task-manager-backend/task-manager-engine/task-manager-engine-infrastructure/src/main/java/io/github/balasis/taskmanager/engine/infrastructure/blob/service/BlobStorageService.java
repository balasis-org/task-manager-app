package io.github.balasis.taskmanager.engine.infrastructure.blob.service;


import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobContainerClient;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@AllArgsConstructor
public class BlobStorageService {
    private final SecretClientProvider secretClientProvider;
    private final BlobServiceClient blobServiceClient;

    public String upload(MultipartFile file) throws IOException {
        String containerName = secretClientProvider.getSecret("TASKMANAGER-BLOB-CONTAINERS-NAME");
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) containerClient.create();

        String blobName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.upload(file.getInputStream(), file.getSize(), true);

        String fullUrl = blobClient.getBlobUrl();
        return fullUrl.substring(fullUrl.indexOf(containerName) + containerName.length() + 1);
    }
}
