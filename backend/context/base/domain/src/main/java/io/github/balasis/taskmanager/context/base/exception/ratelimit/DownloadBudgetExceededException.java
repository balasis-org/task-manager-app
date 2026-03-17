package io.github.balasis.taskmanager.context.base.exception.ratelimit;

// thrown when the group owner's monthly download budget is exhausted.
// the budget is tracked on the User entity (usedDownloadBytesMonth) and
// reset by the maintenance full-cleanup job each month.
// like RepeatDownloadBlockedException this extends RuntimeException directly.
public class DownloadBudgetExceededException extends RuntimeException {
    public DownloadBudgetExceededException(String message) {
        super(message);
    }
}
