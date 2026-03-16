-- V9: Async image moderation queue + user upload/account ban fields
-- ==================================================================
-- Images are uploaded immediately and enqueued as PENDING rows.
-- A scheduled drainer scans them via Azure Content Safety (10 TPS)
-- and marks them APPROVED or REJECTED.  On rejection the blob is
-- deleted and imgUrl reverted.  Maintenance purges processed rows
-- after 30 days (matches the violation escalation window).

CREATE TABLE ImageModerationQueue (
    id                BIGINT        IDENTITY(1,1) PRIMARY KEY,
    userId            BIGINT        NOT NULL,
    entityType        VARCHAR(20)   NOT NULL,   -- 'USER' or 'GROUP'
    entityId          BIGINT        NOT NULL,
    newBlobName       NVARCHAR(500) NOT NULL,
    previousBlobName  NVARCHAR(500) NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    rejectedCategory  VARCHAR(30)   NULL,
    rejectedSeverity  INT           NULL,
    retryCount        INT           NOT NULL DEFAULT 0,
    createdAt         DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    processedAt       DATETIME2     NULL,
    CONSTRAINT CK_IMQ_Status     CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'FAILED')),
    CONSTRAINT CK_IMQ_EntityType CHECK (entityType IN ('USER', 'GROUP'))
);

CREATE INDEX IX_IMQ_status_createdAt ON ImageModerationQueue (status, createdAt);
CREATE INDEX IX_IMQ_userId_status    ON ImageModerationQueue (userId, status, createdAt);

-- 4-level escalation fields on Users
ALTER TABLE [Users] ADD [uploadBannedUntil]  DATETIME2 NULL;
ALTER TABLE [Users] ADD [accountBannedUntil] DATETIME2 NULL;
ALTER TABLE [Users] ADD [uploadBanCount]     INT       NOT NULL DEFAULT 0;
ALTER TABLE [Users] ADD [lastUploadBanAt]    DATETIME2 NULL;

-- ===================================================================
-- Comment Intelligence — TEAMS_PRO tier, PII flag, analysis queue & snapshot
-- ===================================================================
-- Adds:
--   1. containsPii flag on TaskComments (local PII regex, all tiers)
--   2. usedTaskAnalysisCreditsMonth on Users (monthly credit counter)
--   3. TaskAnalysisRequests outbox table (drainer queue)
--   4. TaskAnalysisSnapshots table (one-per-task cached results)

-- 1. PII flag on comments
ALTER TABLE [TaskComments] ADD [containsPii] BIT NOT NULL DEFAULT 0;

-- 2. Monthly analysis credit counter on users
ALTER TABLE [Users] ADD [usedTaskAnalysisCreditsMonth] INT NOT NULL DEFAULT 0;

-- 3. Analysis request queue (outbox pattern, same as ImageModerationQueue)
CREATE TABLE TaskAnalysisRequests (
    id                  BIGINT        IDENTITY(1,1) PRIMARY KEY,
    taskId              BIGINT        NOT NULL,
    groupId             BIGINT        NOT NULL,
    requestedByUserId   BIGINT        NOT NULL,
    analysisType        VARCHAR(20)   NOT NULL,
    creditsCharged      INT           NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    retryCount          INT           NOT NULL DEFAULT 0,
    createdAt           DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    processedAt         DATETIME2     NULL,
    CONSTRAINT CK_TAR_Status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT CK_TAR_Type   CHECK (analysisType IN ('ANALYSIS_ONLY', 'SUMMARY_ONLY', 'FULL'))
);

CREATE INDEX IX_TAR_status    ON TaskAnalysisRequests (status);
CREATE INDEX IX_TAR_task      ON TaskAnalysisRequests (taskId);

-- 4. Analysis snapshot (one per task, upserted)
CREATE TABLE TaskAnalysisSnapshots (
    id                          BIGINT        IDENTITY(1,1) PRIMARY KEY,
    taskId                      BIGINT        NOT NULL,

    -- estimate cache
    estimatedCommentCount       INT           NOT NULL DEFAULT 0,
    estimatedTotalChars         BIGINT        NOT NULL DEFAULT 0,
    estimatedAnalysisCredits    INT           NOT NULL DEFAULT 0,
    estimatedSummaryCredits     INT           NOT NULL DEFAULT 0,
    estimatedEgressCredits      INT           NOT NULL DEFAULT 0,
    estimatedAt                 DATETIME2     NULL,
    estimateChangeMarker        DATETIME2     NULL,

    -- analysis results (nullable until analysis run)
    overallSentiment            VARCHAR(10)   NULL,
    overallConfidence           FLOAT         NULL,
    positiveCount               INT           NULL,
    neutralCount                INT           NULL,
    negativeCount               INT           NULL,
    keyPhrases                  NVARCHAR(MAX) NULL,
    piiDetectedCount            INT           NULL,
    commentResults              NVARCHAR(MAX) NULL,
    analysisCommentCount        INT           NULL,
    analyzedAt                  DATETIME2     NULL,
    analysisChangeMarker        DATETIME2     NULL,

    -- summary results (nullable until summary run)
    summaryText                 NVARCHAR(MAX) NULL,
    summaryCommentCount         INT           NULL,
    summarizedAt                DATETIME2     NULL,
    summaryChangeMarker         DATETIME2     NULL,

    CONSTRAINT UQ_TAS_taskId UNIQUE (taskId)
);

CREATE UNIQUE INDEX IX_TAS_task ON TaskAnalysisSnapshots (taskId);

-- ===================================================================
-- Downgrade grace period + shield support
-- ===================================================================
-- Adds:
--   1. downgradeGraceDeadline on Users — when the 7-day grace period expires
--   2. previousPlan on Users — the plan before the downgrade
--   3. downgradeShielded on Groups — protected from priority deletion

ALTER TABLE [Users] ADD [downgradeGraceDeadline] DATETIME2 NULL;
ALTER TABLE [Users] ADD [previousPlan]           NVARCHAR(20) NULL;

ALTER TABLE [Groups] ADD [downgradeShielded]     BIT NOT NULL DEFAULT 0;
