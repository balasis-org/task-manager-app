package io.github.balasis.taskmanager.engine.infrastructure.redis;

// Best-effort per-file download guard. Uses one Redis hash per user
// with a shared 8-hour window. File IDs are hash fields, values are
// download counts. When the window expires the entire hash is deleted
// — no stale field accumulation. ETag 304s bypass this entirely;
// only actual blob reads are counted (3 per file per window).
// Failures are swallowed — Redis being down never blocks a download.
public interface DownloadGuardService {

    // Checks whether this user downloaded the file recently. Throws
    // RepeatDownloadBlockedException if the cooldown has not expired.
    // On success, records the download so future checks within the
    // cooldown window will block.
    void checkRepeatDownload(long userId, long fileId);
}
