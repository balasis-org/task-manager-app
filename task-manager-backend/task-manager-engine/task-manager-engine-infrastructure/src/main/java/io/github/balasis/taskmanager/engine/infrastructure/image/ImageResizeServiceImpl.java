package io.github.balasis.taskmanager.engine.infrastructure.image;

import io.github.balasis.taskmanager.context.base.exception.blob.upload.BlobUploadImageException;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Service
public class ImageResizeServiceImpl implements ImageResizeService {

    /** Reject anything above 25 megapixels to guard against decompression bombs. */
    private static final long MAX_PIXELS = 25_000_000;

    @Override
    public byte[] resize(MultipartFile file, int size) {
        try {
            return resize(file.getBytes(), size);
        } catch (BlobUploadImageException e) {
            throw e;
        } catch (IOException e) {
            throw new BlobUploadImageException("Failed to process image");
        }
    }

    @Override
    public byte[] resize(byte[] raw, int size) {
        try {
            checkPixelDimensions(raw);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(raw))
                    .size(size, size)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(out);
            return out.toByteArray();

        } catch (BlobUploadImageException e) {
            throw e;
        } catch (IOException e) {
            throw new BlobUploadImageException("Failed to process image");
        }
    }

    private void checkPixelDimensions(byte[] data) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (iis == null) {
                throw new BlobUploadImageException("Could not read image");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new BlobUploadImageException("Unsupported image format");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > MAX_PIXELS) {
                    throw new BlobUploadImageException(
                            "Image dimensions too large (max ~5000\u00d75000 pixels)");
                }
            } finally {
                reader.dispose();
            }
        }
    }
}
