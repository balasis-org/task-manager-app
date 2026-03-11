package io.github.balasis.taskmanager.engine.infrastructure.blob.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.PublicAccessType;
import io.github.balasis.taskmanager.context.base.exception.blob.upload.BlobUploadException;
import io.github.balasis.taskmanager.context.base.exception.blob.upload.BlobUploadImageException;
import io.github.balasis.taskmanager.context.base.exception.blob.upload.BlobUploadTaskFileException;
import io.github.balasis.taskmanager.context.base.exception.critical.CriticalBlobStorageException;
import io.github.balasis.taskmanager.context.base.limits.PlanLimits;
import io.github.balasis.taskmanager.context.base.utils.StringSanitizer;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.ContentSafetyService;
import io.github.balasis.taskmanager.engine.infrastructure.image.ImageResizeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class BlobStorageService {

    private static final Logger log = LoggerFactory.getLogger(BlobStorageService.class);

    private final ContentSafetyService contentSafetyService;
    private final ImageResizeService imageResizeService;

    private final Map<BlobContainerType, BlobContainerClient> containers =
            new EnumMap<>(BlobContainerType.class);

    public BlobStorageService(BlobServiceClient blobServiceClient,
                              ContentSafetyService contentSafetyService,
                              ImageResizeService imageResizeService) {
        this.contentSafetyService = contentSafetyService;
        this.imageResizeService = imageResizeService;

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

    public BlobDownload downloadTaskAssigneeFile(String blobName){
        return downloadInternal(BlobContainerType.TASK_ASSIGNEE_FILES, blobName);
    }

    public String uploadTaskAssigneeFile(MultipartFile file, Long prefixId, long maxSizeBytes){
        assertTaskAssigneeFile(file, maxSizeBytes);
        return uploadInternal(BlobContainerType.TASK_ASSIGNEE_FILES, file, prefixId);
    }

    public BlobDownload downloadTaskFile(String blobName){
        return downloadInternal(BlobContainerType.TASK_FILES, blobName);
    }

    public String uploadTaskFile(MultipartFile file, Long prefixId, long maxSizeBytes){
        assertTaskFile(file, maxSizeBytes);
        return uploadInternal(BlobContainerType.TASK_FILES, file, prefixId);
    }

    public String uploadProfileImage(MultipartFile file, Long prefixId){
        validateImageFormat(file);
        byte[] resized = imageResizeService.resize(file, ImageResizeService.PROFILE_SIZE);
        assertContentSafety(resized);
        return uploadBytes(BlobContainerType.PROFILE_IMAGES, resized, prefixId, file.getOriginalFilename());
    }

    public String uploadGroupImage(MultipartFile file, Long prefixId){
        validateImageFormat(file);
        byte[] resized = imageResizeService.resize(file, ImageResizeService.GROUP_SIZE);
        assertContentSafety(resized);
        return uploadBytes(BlobContainerType.GROUP_IMAGES, resized, prefixId, file.getOriginalFilename());
    }

    /**
     * Uploads a pre-trusted image (e.g. Microsoft profile photo) to the
     * profile-images container. Resizes but skips Content Safety —
     * the source is already moderated by Microsoft.
     */
    public String uploadTrustedProfileImage(byte[] imageBytes, Long userId) {
        byte[] resized = imageResizeService.resize(imageBytes, ImageResizeService.PROFILE_SIZE);
        return uploadBytes(BlobContainerType.PROFILE_IMAGES, resized, userId, "ms-photo.jpg");
    }

    private String uploadInternal(BlobContainerType type, MultipartFile file, Long prefixId){
        BlobContainerClient container = containers.get(type);
        String blobName = StringSanitizer.toSafeBlobKey(prefixId,file.getOriginalFilename());
        BlobClient blobClient = container.getBlobClient(blobName);
        try {
            blobClient.upload(file.getInputStream(), file.getSize(), true);
        } catch (Exception e) {
            throw new CriticalBlobStorageException("Blob upload failed: " + e.getMessage());
        }

        return blobName;
    }

    private BlobDownload downloadInternal(BlobContainerType type, String blobName){
        BlobContainerClient container = containers.get(type);
        BlobClient blobClient = container.getBlobClient(blobName);
        if (!blobClient.exists()) {
            throw new RuntimeException("Blob not found: " + blobName);
        }
        long size = blobClient.getProperties().getBlobSize();
        var inputStream = blobClient.openInputStream();
        return new BlobDownload(inputStream, size);
    }

    public record BlobDownload(java.io.InputStream inputStream, long size) {}

    private void deleteInternal(BlobContainerType type, String blobName) {
        BlobContainerClient container = containers.get(type);
        BlobClient blobClient = container.getBlobClient(blobName);
        if (blobClient.exists()) {
            blobClient.delete();
        }
    }

    /**
     * True fire-and-forget: dispatches the delete to a background thread
     * and returns immediately.  The caller is never blocked.  If the
     * delete fails for any reason the orphan blob will be swept by
     * the next scheduled maintenance run.
     */
    private void tryDeleteAsync(BlobContainerType type, String blobName) {
        if (blobName == null || blobName.isBlank()) return;
        CompletableFuture.runAsync(() -> deleteInternal(type, blobName))
                .exceptionally(ex -> {
                    log.warn("Async blob delete failed for {}/{}: {}",
                            type.getContainerName(), blobName, ex.getMessage());
                    return null;
                });
    }

    public void deleteProfileImage(String blobName) {
        tryDeleteAsync(BlobContainerType.PROFILE_IMAGES, blobName);
    }

    public void deleteGroupImage(String blobName) {
        tryDeleteAsync(BlobContainerType.GROUP_IMAGES, blobName);
    }

    public void deleteTaskFile(String blobName) {
        if (blobName == null || blobName.isBlank()) return;
        deleteInternal(BlobContainerType.TASK_FILES, blobName);
    }

    public void deleteTaskAssigneeFile(String blobName) {
        if (blobName == null || blobName.isBlank()) return;
        deleteInternal(BlobContainerType.TASK_ASSIGNEE_FILES, blobName);
    }

    private void assertTaskAssigneeFile(MultipartFile file, long maxSizeBytes) {
        if (file == null || file.isEmpty()) {
            throw new BlobUploadTaskFileException("TaskAssignee file is empty");
        }

        if (file.getSize() > PlanLimits.HARD_CAP_FILE_SIZE_BYTES) {
            throw new BlobUploadTaskFileException("File exceeds the absolute maximum of 100 MB");
        }

        if (file.getSize() > maxSizeBytes) {
            throw new BlobUploadTaskFileException(
                    "TaskAssignee file exceeds max size of " + (maxSizeBytes / (1024 * 1024)) + " MB");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw new BlobUploadTaskFileException("TaskAssignee file must have a name");
        }

    }

    private void assertTaskFile(MultipartFile file, long maxSizeBytes) {
        if (file == null || file.isEmpty()) {
            throw new BlobUploadTaskFileException("Task file is empty");
        }

        if (file.getSize() > PlanLimits.HARD_CAP_FILE_SIZE_BYTES) {
            throw new BlobUploadTaskFileException("File exceeds the absolute maximum of 100 MB");
        }

        if (file.getSize() > maxSizeBytes) {
            throw new BlobUploadTaskFileException(
                    "Task file exceeds max size of " + (maxSizeBytes / (1024 * 1024)) + " MB");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw new BlobUploadTaskFileException("Task file must have a name");
        }

    }

    private void validateImageFormat(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BlobUploadImageException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BlobUploadImageException("Only image files are allowed");
        }

        if ("image/gif".equals(contentType)) {
            throw new BlobUploadImageException("GIF images are not supported. Please use PNG or JPG.");
        }

        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BlobUploadImageException("Image too large");
        }
    }

    private void assertContentSafety(byte[] imageBytes) {
        if (!contentSafetyService.isSafe(new ByteArrayInputStream(imageBytes))) {
            throw new BlobUploadImageException("Image failed content safety check (potential adult content)");
        }
    }

    private String uploadBytes(BlobContainerType type, byte[] data, Long prefixId, String originalFilename) {
        BlobContainerClient container = containers.get(type);
        String blobName = StringSanitizer.toSafeBlobKey(prefixId, originalFilename);
        BlobClient blobClient = container.getBlobClient(blobName);
        try {
            blobClient.upload(new ByteArrayInputStream(data), data.length, true);
        } catch (Exception e) {
            throw new CriticalBlobStorageException("Blob upload failed: " + e.getMessage());
        }
        return blobName;
    }
}
