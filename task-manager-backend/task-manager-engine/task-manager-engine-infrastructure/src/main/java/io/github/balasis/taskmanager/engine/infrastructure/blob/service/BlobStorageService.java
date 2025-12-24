package io.github.balasis.taskmanager.engine.infrastructure.blob.service;


import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.PublicAccessType;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class BlobStorageService {

    private static final String TASK_FILE_CONTAINER = "task-files";
    private static final String PROFILE_IMG_CONTAINER = "profile-images";
    private static final String GROUP_IMG_CONTAINER = "group-images";
    private static final String TASK_IMG_CONTAINER = "task-images";

    private final BlobContainerClient taskFileContainer;
    private final BlobContainerClient profileImgContainer;
    private final BlobContainerClient groupImgContainer;
    private final BlobContainerClient taskImgContainer;

    public BlobStorageService(BlobServiceClient blobServiceClient) {
        this.taskFileContainer = initContainer(blobServiceClient, TASK_FILE_CONTAINER, false);
        this.profileImgContainer = initContainer(blobServiceClient, PROFILE_IMG_CONTAINER, true);
        this.groupImgContainer = initContainer(blobServiceClient, GROUP_IMG_CONTAINER, true);
        this.taskImgContainer = initContainer(blobServiceClient, TASK_IMG_CONTAINER, true);
    }

    private BlobContainerClient initContainer(
            BlobServiceClient serviceClient,
            String containerName,
            boolean isPublic
    ) {
        BlobContainerClient container = serviceClient.getBlobContainerClient(containerName);

        if (!container.exists()) {
            container.create();
            if (isPublic) {
                container.setAccessPolicy(PublicAccessType.BLOB, null);
            }
        }

        return container;
    }

    private String uploadInternal(BlobContainerClient container, MultipartFile file) throws IOException {
        String blobName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        BlobClient blobClient = container.getBlobClient(blobName);
        blobClient.upload(file.getInputStream(), file.getSize(), true);
        return blobName;
    }

    private byte[] downloadInternal(BlobContainerClient container, String blobName) throws IOException {
        BlobClient blobClient = container.getBlobClient(blobName);
        if (!blobClient.exists()) {
            throw new RuntimeException("Blob not found: " + blobName);
        }
        try (var inputStream = blobClient.openInputStream()) {
            return inputStream.readAllBytes();
        }
    }

    private void deleteInternal(BlobContainerClient container, String blobName) {
        BlobClient blobClient = container.getBlobClient(blobName);
        if (blobClient.exists()) {
            blobClient.delete();
        }
    }

    public String uploadTaskFile(MultipartFile file) throws IOException {
        return uploadInternal(taskFileContainer, file);
    }

    public byte[] downloadTaskFile(String blobName) throws IOException {
        return downloadInternal(taskFileContainer, blobName);
    }

    public void deleteTaskFile(String blobName) {
        deleteInternal(taskFileContainer, blobName);
    }

    public String uploadProfileImage(MultipartFile file) throws IOException {
        return uploadInternal(profileImgContainer, file);
    }

    public void deleteProfileImage(String blobName) {
        deleteInternal(profileImgContainer, blobName);
    }

    public String uploadGroupImage(MultipartFile file) throws IOException {
        return uploadInternal(groupImgContainer, file);
    }

    public void deleteGroupImage(String blobName) {
        deleteInternal(groupImgContainer, blobName);
    }

    public String uploadTaskImage(MultipartFile file) throws IOException {
        return uploadInternal(taskImgContainer, file);
    }

    public void deleteTaskImage(String blobName) {
        deleteInternal(taskImgContainer, blobName);
    }


}
