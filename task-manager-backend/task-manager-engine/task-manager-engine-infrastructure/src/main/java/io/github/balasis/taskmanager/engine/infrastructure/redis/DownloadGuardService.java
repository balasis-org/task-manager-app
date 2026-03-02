package io.github.balasis.taskmanager.engine.infrastructure.redis;

// Best-effort per-file daily download cap. Uses Redis INCR with 24h TTL
// to limit how often the same user can re-download the same file.
// Failures are swallowed — Redis being down should never block a download.
public interface DownloadGuardService {

    // Increments the counter for this user+file pair. Throws
    // RepeatDownloadBlockedException if the daily cap is exceeded.
    void checkRepeatDownload(long userId, long fileId);
}
