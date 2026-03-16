package io.github.balasis.taskmanager.engine.infrastructure.contentsafety.service;

import com.azure.ai.contentsafety.ContentSafetyClient;
import com.azure.ai.contentsafety.models.*;
import com.azure.core.util.BinaryData;
import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.blob.upload.BlobUploadException;
import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.ContentSafetyService;
import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.ModerationResult;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ContentSafetyServiceImpl extends BaseComponent implements ContentSafetyService {

    private final ContentSafetyClient client;

    private static final int MAX_IMAGE_BYTES = 4 * 1024 * 1024;

    // Per-category severity thresholds — at or above this value = violation
    private static final Map<ImageCategory, Integer> THRESHOLDS = Map.of(
            ImageCategory.SEXUAL,    2,  // strict
            ImageCategory.VIOLENCE,  4,  // lenient (anime/game art tolerance)
            ImageCategory.HATE,      4,
            ImageCategory.SELF_HARM, 4
    );

    private static final int DEFAULT_THRESHOLD = 4;

    public ContentSafetyServiceImpl(ContentSafetyClient client) {
        this.client = client;
    }

    @Override
    public ModerationResult analyze(InputStream input) {
        try {
            byte[] bytes = readCapped(input, MAX_IMAGE_BYTES);
            var imageData = new ContentSafetyImageData()
                    .setContent(BinaryData.fromBytes(bytes));

            var options = new AnalyzeImageOptions(imageData);
            var result = client.analyzeImage(options);

            List<ImageCategoriesAnalysis> categories = result.getCategoriesAnalysis();

            for (ImageCategoriesAnalysis c : categories) {
                logger.trace("{} severity: {}", c.getCategory(), c.getSeverity());
            }

            for (ImageCategoriesAnalysis c : categories) {
                int severity = c.getSeverity() != null ? c.getSeverity() : 0;
                int threshold = THRESHOLDS.getOrDefault(c.getCategory(), DEFAULT_THRESHOLD);
                if (severity >= threshold) {
                    return ModerationResult.rejected(
                            c.getCategory().toString(), severity);
                }
            }

            return ModerationResult.safe();

        } catch (IOException e) {
            throw new BlobUploadException("Failed reading image: " + e.getMessage());
        }
    }

    private static byte[] readCapped(InputStream in, int maxBytes) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(Math.min(maxBytes, 8192));
        byte[] chunk = new byte[8192];
        int total = 0;
        int read;
        while ((read = in.read(chunk)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new BlobUploadException("Image exceeds safety-check size limit");
            }
            buf.write(chunk, 0, read);
        }
        return buf.toByteArray();
    }

}
