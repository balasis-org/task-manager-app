package io.github.balasis.taskmanager.context.base.exception.ratelimit;

public class RepeatDownloadBlockedException extends RuntimeException {
    public RepeatDownloadBlockedException(String message) {
        super(message);
    }
}
