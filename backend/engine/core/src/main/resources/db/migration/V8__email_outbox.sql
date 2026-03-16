-- V8: Transactional Outbox for rate-limited email delivery
-- ========================================================
-- Emails are enqueued as PENDING rows and drained by a
-- scheduled worker that respects ACS platform rate limits
-- (30/min, 100/hour).  Maintenance purges SENT/FAILED rows
-- older than 24 hours.

CREATE TABLE EmailOutbox (
    id         BIGINT        IDENTITY(1,1) PRIMARY KEY,
    toAddress  NVARCHAR(320) NOT NULL,
    subject    NVARCHAR(500) NOT NULL,
    body       NVARCHAR(MAX) NOT NULL,
    status     VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    createdAt  DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    sentAt     DATETIME2     NULL,
    retryCount INT           NOT NULL DEFAULT 0,
    CONSTRAINT CK_EmailOutbox_Status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX IX_EmailOutbox_status_createdAt ON EmailOutbox (status, createdAt);
CREATE INDEX IX_EmailOutbox_sentAt           ON EmailOutbox (sentAt) WHERE sentAt IS NOT NULL;
