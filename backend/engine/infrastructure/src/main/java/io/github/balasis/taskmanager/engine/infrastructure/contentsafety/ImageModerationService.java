package io.github.balasis.taskmanager.engine.infrastructure.contentsafety;

// entry point for queueing an image for async moderation.
// the core layer (JpaImageModerationService) writes a PENDING row;
// the ImageModerationDrainer picks it up on the next 10s tick.
public interface ImageModerationService {
    void enqueue(Long userId, String entityType, Long entityId,
                 String newBlobName, String previousBlobName);
}
