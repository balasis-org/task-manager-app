package io.github.balasis.taskmanager.engine.infrastructure.image;

import org.springframework.web.multipart.MultipartFile;

/**
 * Resizes uploaded images to the exact display dimensions before
 * Content Safety scanning and blob storage. Reduces egress, storage,
 * and CS payload size by 90-95%.
 */
public interface ImageResizeService {

    /** Profile avatars — displayed at 7 em (≈112 px); 256 px covers 2× retina. */
    int PROFILE_SIZE = 256;

    /** Group thumbnails — displayed at 3 em (≈48 px); 256 px covers 3×+ retina. */
    int GROUP_SIZE = 256;

    /**
     * Reads the uploaded image, validates pixel dimensions, and returns
     * a JPEG byte array resized to {@code size × size} at 85 % quality.
     */
    byte[] resize(MultipartFile file, int size);

    /** Same as {@link #resize(MultipartFile, int)} but from raw bytes. */
    byte[] resize(byte[] imageBytes, int size);
}
