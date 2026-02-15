package io.github.balasis.taskmanager.engine.infrastructure.blob.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.PublicAccessType;
import io.github.balasis.taskmanager.context.base.exception.blob.upload.BlobUploadException;
import io.github.balasis.taskmanager.context.base.exception.blob.upload.BlobUploadImageException;
import io.github.balasis.taskmanager.context.base.exception.blob.upload.BlobUploadTaskFileException;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.ContentSafetyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

@Service
public class BlobStorageService {
    private final ContentSafetyService contentSafetyService;

    private final Map<BlobContainerType, BlobContainerClient> containers =
            new EnumMap<>(BlobContainerType.class);

    public BlobStorageService(BlobServiceClient blobServiceClient,
                              ContentSafetyService contentSafetyService) {
        this.contentSafetyService = contentSafetyService;

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

    public byte[] downloadTaskFile(String blobName){
        return downloadInternal(BlobContainerType.TASK_FILES, blobName);
    }

    public String uploadTaskFile(MultipartFile file , Long prefixId){
        assertTaskFile(file);
        return uploadInternal(BlobContainerType.TASK_FILES, file, prefixId);
    }

    public String uploadProfileImage(MultipartFile file, Long prefixId){
        assertImage(file);
        return uploadInternal(BlobContainerType.PROFILE_IMAGES, file, prefixId);
    }

    public String uploadGroupImage(MultipartFile file, Long prefixId){
        assertImage(file);
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

    private String uploadInternal(BlobContainerType type, MultipartFile file, Long prefixId){
        BlobContainerClient container = containers.get(type);
        String blobName = prefixId + "-" + file.getOriginalFilename();
        BlobClient blobClient = container.getBlobClient(blobName);
        try {
            blobClient.upload(file.getInputStream(), file.getSize(), true);
        } catch (IOException e) {
            throw new BlobUploadException(e.getMessage());
        }

        return blobName;
    }

    private byte[] downloadInternal(BlobContainerType type, String blobName){
        BlobContainerClient container = containers.get(type);
        BlobClient blobClient = container.getBlobClient(blobName);
        if (!blobClient.exists()) {
            throw new RuntimeException("Blob not found: " + blobName);
        }
        try (var inputStream = blobClient.openInputStream()) {
            try {
                return inputStream.readAllBytes();
            } catch (IOException e) {
                throw new BlobUploadException(e.getMessage());
            }
        }
    }

    private void deleteInternal(BlobContainerType type, String blobName) {
        BlobContainerClient container = containers.get(type);
        BlobClient blobClient = container.getBlobClient(blobName);
        if (blobClient.exists()) {
            blobClient.delete();
        }
    }

    private void assertTaskFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BlobUploadTaskFileException("Task file is empty");
        }

        long maxSize = 40L * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BlobUploadTaskFileException("Task file exceeds max size of 40MB");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw new BlobUploadTaskFileException("Task file must have a name");
        }
    }

    private void assertImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BlobUploadImageException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BlobUploadImageException("Only image files are allowed");
        }


        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BlobUploadImageException("Image too large");
        }

        try {
            if (!contentSafetyService.isSafe(file.getInputStream())) {
                throw new BlobUploadImageException("Image failed content safety check (potential adult content)");
            }
        } catch (IOException e) {
            throw new BlobUploadImageException("Failed reading image for safety check: " + e.getMessage());
        }

    }
}
