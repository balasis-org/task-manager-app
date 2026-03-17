package io.github.balasis.taskmanager.context.base.exception.blob.upload;

// thrown when we try to enqueue an image for moderation but the user has
// exhausted their monthly Content Safety scan budget. the image upload is
// rejected before it even reaches the moderation queue.
public class BlobUploadContentSafetyException extends BlobUploadException {
    public BlobUploadContentSafetyException(String message) {
        super(message);
    }
}
