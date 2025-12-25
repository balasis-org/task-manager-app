package io.github.balasis.taskmanager.engine.infrastructure.blob.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.PublicAccessType;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

@Service
public class BlobStorageService {

    private final Map<BlobContainerType, BlobContainerClient> containers =
            new EnumMap<>(BlobContainerType.class);

    public BlobStorageService(BlobServiceClient blobServiceClient) {
        for (BlobContainerType type : BlobContainerType.values()) {
            BlobContainerClient container =
                    blobServiceClient.getBlobContainerClient(type.getContainerName());

            if (!container.exists()) {
                container.create();
                if (type.isPublic()) {
                    container.setAccessPolicy(PublicAccessType.BLOB, null);
                }
            }

            containers.put(type, container);
        }
    }



    public byte[] downloadTaskFile(String blobName) throws IOException {
        return downloadInternal(BlobContainerType.TASK_FILES, blobName);
    }

    public String uploadTaskFile(MultipartFile file , Long prefixId) throws IOException {
        return uploadInternal(BlobContainerType.TASK_FILES, file, prefixId);
    }

    public String uploadProfileImage(MultipartFile file, Long prefixId) throws IOException {
        return uploadInternal(BlobContainerType.PROFILE_IMAGES, file, prefixId);
    }

    public String uploadTaskImage(MultipartFile file, Long prefixId) throws IOException {
        return uploadInternal(BlobContainerType.TASK_IMAGES, file, prefixId);
    }

    public String uploadGroupImage(MultipartFile file, Long prefixId) throws IOException {
        return uploadInternal(BlobContainerType.GROUP_IMAGES, file, prefixId);
    }

    public void deleteTaskFile(String blobName) {
        deleteInternal(BlobContainerType.TASK_FILES, blobName);
    }

    public void deleteGroupImage(String blobName) {
        deleteInternal(BlobContainerType.GROUP_IMAGES, blobName);
    }

    public void deleteProfileImage(String blobName) {
        deleteInternal(BlobContainerType.PROFILE_IMAGES, blobName);
    }

    public void deleteTaskImage(String blobName) {
        deleteInternal(BlobContainerType.TASK_IMAGES, blobName);
    }

    private String uploadInternal(BlobContainerType type, MultipartFile file, Long prefixId) throws IOException {
        BlobContainerClient container = containers.get(type);
        String blobName = prefixId + "-" + file.getOriginalFilename();
        BlobClient blobClient = container.getBlobClient(blobName);
        blobClient.upload(file.getInputStream(), file.getSize(), true);
        return blobName;
    }

    private byte[] downloadInternal(BlobContainerType type, String blobName) throws IOException {
        BlobContainerClient container = containers.get(type);
        BlobClient blobClient = container.getBlobClient(blobName);
        if (!blobClient.exists()) {
            throw new RuntimeException("Blob not found: " + blobName);
        }
        try (var inputStream = blobClient.openInputStream()) {
            return inputStream.readAllBytes();
        }
    }

    private void deleteInternal(BlobContainerType type, String blobName) {
        BlobContainerClient container = containers.get(type);
        BlobClient blobClient = container.getBlobClient(blobName);
        if (blobClient.exists()) {
            blobClient.delete();
        }
    }
}
