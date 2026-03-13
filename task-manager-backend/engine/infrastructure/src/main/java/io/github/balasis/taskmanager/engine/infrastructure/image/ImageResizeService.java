package io.github.balasis.taskmanager.engine.infrastructure.image;

import org.springframework.web.multipart.MultipartFile;

/** Pre-sizes uploaded images before CS scan + blob storage. */
public interface ImageResizeService {

    /** Profile avatars — displayed at 7 em (≈112 px); 256 px covers 2× retina. */
    int PROFILE_SIZE = 256;

    /** Group thumbnails — displayed at 3 em (≈48 px); 256 px covers 3×+ retina. */
    int GROUP_SIZE = 256;

    /** Returns a {@code size × size} JPEG at 85 % quality. */
    byte[] resize(MultipartFile file, int size);

    /** Same as {@link #resize(MultipartFile, int)} but from raw bytes. */
    byte[] resize(byte[] imageBytes, int size);
}
