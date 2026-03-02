
-- ============================================================
-- V4: Subscription tiers, storage budget & task-level overrides
-- ============================================================

-- 1. Widen the subscription_plan CHECK constraint
--    Old values: 'FREE', 'PREMIUM'
--    New values: 'FREE', 'STUDENT', 'ORGANIZER', 'TEAM'
ALTER TABLE [Users] DROP CONSTRAINT [CK_Users_subscriptionPlan];

ALTER TABLE [Users] WITH CHECK ADD CONSTRAINT [CK_Users_subscriptionPlan]
    CHECK ([subscriptionPlan] IN ('FREE','STUDENT','ORGANIZER','TEAM'));

-- 2. Migrate existing PREMIUM users → STUDENT (closest tier)
UPDATE [Users] SET [subscriptionPlan] = 'STUDENT' WHERE [subscriptionPlan] = 'PREMIUM';

-- 3. Storage-budget counter on Users (atomic counter for paid-tier uploads)
ALTER TABLE [Users]
    ADD [usedStorageBytes] BIGINT NOT NULL DEFAULT 0;

-- 4. Track individual file sizes so maintenance can reconcile counters
ALTER TABLE [TaskFiles]
    ADD [fileSize] BIGINT NULL;

ALTER TABLE [TaskAssigneeFiles]
    ADD [fileSize] BIGINT NULL;

-- 5. Group-level override columns (nullable = "use plan default")
--    Group leaders can tighten limits below their plan cap.
ALTER TABLE [Groups]
    ADD [maxCreatorFilesPerTask]  INT    NULL;

ALTER TABLE [Groups]
    ADD [maxAssigneeFilesPerTask] INT    NULL;

ALTER TABLE [Groups]
    ADD [maxFileSizeBytes]        BIGINT NULL;

-- 6. Task-level override columns (nullable = "use group / plan default")
--    Whoever creates the task can set per-task limits.
ALTER TABLE [Tasks]
    ADD [maxCreatorFiles]     INT    NULL;

ALTER TABLE [Tasks]
    ADD [maxAssigneeFiles]    INT    NULL;

ALTER TABLE [Tasks]
    ADD [maxFileSizeBytes]    BIGINT NULL;
