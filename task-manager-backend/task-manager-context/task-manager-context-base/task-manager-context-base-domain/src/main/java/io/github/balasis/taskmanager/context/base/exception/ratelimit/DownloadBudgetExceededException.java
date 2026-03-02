package io.github.balasis.taskmanager.context.base.exception.ratelimit;

public class DownloadBudgetExceededException extends RuntimeException {
    public DownloadBudgetExceededException(String message) {
        super(message);
    }
}
