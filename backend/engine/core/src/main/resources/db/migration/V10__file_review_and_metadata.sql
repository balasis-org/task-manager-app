-- V10: Per-file review status + file metadata (uploader, timestamp)
-- ==================================================================
-- Adds:
--   1. uploadedBy + createdAt columns on TaskFiles and TaskAssigneeFiles
--      so the Group File Manager can show who uploaded when.
--   2. FileReviewStatuses table — one row per file × reviewer, supporting
--      per-file CHECKED / NEEDS_REVISION feedback during task review.

-- ── 1. File metadata ────────────────────────────────────────────────

ALTER TABLE [TaskFiles] ADD [uploadedById] BIGINT NULL;
ALTER TABLE [TaskFiles] ADD [createdAt]    DATETIME2 NULL;

ALTER TABLE [TaskAssigneeFiles] ADD [uploadedById] BIGINT NULL;
ALTER TABLE [TaskAssigneeFiles] ADD [createdAt]    DATETIME2 NULL;

-- FK constraints (nullable — legacy rows have no uploader)
ALTER TABLE [TaskFiles]
    ADD CONSTRAINT FK_TF_UploadedBy
    FOREIGN KEY ([uploadedById]) REFERENCES [Users]([id]);

ALTER TABLE [TaskAssigneeFiles]
    ADD CONSTRAINT FK_TAF_UploadedBy
    FOREIGN KEY ([uploadedById]) REFERENCES [Users]([id]);

-- ── 2. Per-file review status ───────────────────────────────────────

CREATE TABLE FileReviewStatuses (
    id                      BIGINT        IDENTITY(1,1) PRIMARY KEY,
    taskFileId              BIGINT        NULL,
    taskAssigneeFileId      BIGINT        NULL,
    reviewerId              BIGINT        NOT NULL,
    status                  VARCHAR(20)   NOT NULL,
    note                    NVARCHAR(200) NULL,
    createdAt               DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),

    CONSTRAINT FK_FRS_TaskFile        FOREIGN KEY (taskFileId)         REFERENCES TaskFiles(id)         ON DELETE CASCADE,
    CONSTRAINT FK_FRS_AssigneeFile    FOREIGN KEY (taskAssigneeFileId) REFERENCES TaskAssigneeFiles(id) ON DELETE CASCADE,
    CONSTRAINT FK_FRS_Reviewer        FOREIGN KEY (reviewerId)         REFERENCES [Users](id),
    CONSTRAINT CK_FRS_Status          CHECK (status IN ('CHECKED', 'NEEDS_REVISION')),
    CONSTRAINT CK_FRS_ExactlyOneFile  CHECK (
        (taskFileId IS NOT NULL AND taskAssigneeFileId IS NULL)
        OR (taskFileId IS NULL AND taskAssigneeFileId IS NOT NULL)
    )
);

-- One review per file per reviewer (upsert semantics in application layer)
CREATE UNIQUE INDEX UX_FRS_TaskFile_Reviewer
    ON FileReviewStatuses (taskFileId, reviewerId)
    WHERE taskFileId IS NOT NULL;

CREATE UNIQUE INDEX UX_FRS_AssigneeFile_Reviewer
    ON FileReviewStatuses (taskAssigneeFileId, reviewerId)
    WHERE taskAssigneeFileId IS NOT NULL;

-- Lookup: "all reviews for files in task X" — join through TaskFiles/TaskAssigneeFiles
CREATE INDEX IX_FRS_TaskFile         ON FileReviewStatuses (taskFileId)         WHERE taskFileId IS NOT NULL;
CREATE INDEX IX_FRS_AssigneeFile     ON FileReviewStatuses (taskAssigneeFileId) WHERE taskAssigneeFileId IS NOT NULL;

-- ── 3. Bootstrap lock for multi-instance DataLoader coordination ────

CREATE TABLE [BootstrapLocks] (
    [id]        BIGINT      IDENTITY(1,1) PRIMARY KEY,
    [name]      VARCHAR(50) NOT NULL,
    [completed] BIT         NOT NULL DEFAULT 0,

    CONSTRAINT UQ_BootstrapLocks_Name UNIQUE ([name])
);

INSERT INTO [BootstrapLocks] ([name], [completed]) VALUES ('DATA_LOADER', 0);
INSERT INTO [BootstrapLocks] ([name], [completed]) VALUES ('DEFAULT_IMAGES', 0);

-- ── 4. Allow STALE status in image moderation queue ─────────────────
-- When rapid uploads produce multiple PENDING rows for the same entity,
-- the drainer marks all but the newest as STALE and skips them.

ALTER TABLE [ImageModerationQueue] DROP CONSTRAINT CK_IMQ_Status;
ALTER TABLE [ImageModerationQueue] ADD CONSTRAINT CK_IMQ_Status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'FAILED', 'STALE'));

-- ── 5. Upgrade TaskAnalysisRequests index to composite for ordered batch fetch ─
-- findByStatus('PENDING') ORDER BY createdAt needs a composite index,
-- not just (status) alone. Drop the old one and create proper composite.

DROP INDEX IX_TAR_status ON TaskAnalysisRequests;
CREATE INDEX IX_TAR_status_createdAt ON TaskAnalysisRequests (status, createdAt);
