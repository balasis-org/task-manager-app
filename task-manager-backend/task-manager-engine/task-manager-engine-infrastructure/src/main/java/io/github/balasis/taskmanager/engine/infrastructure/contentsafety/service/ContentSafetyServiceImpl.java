package io.github.balasis.taskmanager.engine.infrastructure.contentsafety.service;

import com.azure.ai.contentsafety.ContentSafetyClient;
import com.azure.ai.contentsafety.models.*;
import com.azure.core.util.BinaryData;
import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.blob.upload.BlobUploadException;
import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.ContentSafetyService;

import java.io.InputStream;
import java.io.IOException;
import java.util.List;

public class ContentSafetyServiceImpl extends BaseComponent implements ContentSafetyService {

    private final ContentSafetyClient client;

    public ContentSafetyServiceImpl(ContentSafetyClient client) {
        this.client = client;
    }

    @Override
    public boolean isSafe(InputStream input) {
        try {
            byte[] bytes = input.readAllBytes();
            var imageData = new ContentSafetyImageData()
                    .setContent(BinaryData.fromBytes(bytes));

            var options = new AnalyzeImageOptions(imageData);
            var result = client.analyzeImage(options);

            List<ImageCategoriesAnalysis> categories = result.getCategoriesAnalysis();

            for (ImageCategoriesAnalysis c : categories) {
                logger.trace("{} severity: {}", c.getCategory(), c.getSeverity());
            }

            return categories.stream()
                    .map(ImageCategoriesAnalysis::getSeverity)
                    .allMatch(severity -> severity == null || severity < 3);

        } catch (IOException e) {
            throw new BlobUploadException("Failed reading image" + e.getMessage());
        }
    }

}
