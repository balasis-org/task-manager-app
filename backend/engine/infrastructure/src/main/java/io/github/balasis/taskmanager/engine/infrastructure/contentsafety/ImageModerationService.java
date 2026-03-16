package io.github.balasis.taskmanager.engine.infrastructure.contentsafety;

public interface ImageModerationService {
    void enqueue(Long userId, String entityType, Long entityId,
                 String newBlobName, String previousBlobName);
}
