package io.github.balasis.taskmanager.context.base.exception.ratelimit;

// thrown when a user tries to download the same file again too quickly.
// the Redis DownloadGuard tracks recent downloads per user+file and blocks
// repeats within a short window to prevent clients hammering the same blob.
// extends RuntimeException directly (not TaskManagerException) because
// GlobalExceptionHandler treats it separately with a 429 + specific message.
public class RepeatDownloadBlockedException extends RuntimeException {
    public RepeatDownloadBlockedException(String message) {
        super(message);
    }
}
