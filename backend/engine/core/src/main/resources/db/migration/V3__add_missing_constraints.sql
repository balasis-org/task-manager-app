
-- ============================================================
-- V3: Add missing unique constraints & normalise column casing
-- ============================================================

-- 1. Rename [Announcement] → [announcement]  (cosmetic – matches the
--    Java field after the Announcement→announcement rename; SQL Server
--    identifiers are case-insensitive so existing queries still work)
EXEC sp_rename 'Groups.Announcement', 'announcement', 'COLUMN';

-- 2. UNIQUE on Groups(name, owner_id)  –  one group-name per owner
;WITH dupes AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY [name], [owner_id] ORDER BY [id] ASC) AS rn
    FROM [Groups]
)
DELETE FROM dupes WHERE rn > 1;

CREATE UNIQUE NONCLUSTERED INDEX [UQ_Groups_name_owner]
    ON [Groups]([name] ASC, [owner_id] ASC);

-- 3. UNIQUE on Tasks(title, group_id)  –  one title per group
;WITH dupes AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY [title], [group_id] ORDER BY [id] ASC) AS rn
    FROM [Tasks]
)
DELETE FROM dupes WHERE rn > 1;

CREATE UNIQUE NONCLUSTERED INDEX [UQ_Tasks_title_group]
    ON [Tasks]([title] ASC, [group_id] ASC);

-- 4. UNIQUE on TaskParticipants(user_id, task_id, task_participant_role)
;WITH dupes AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY [user_id], [task_id], [task_participant_role] ORDER BY [id] ASC) AS rn
    FROM [TaskParticipants]
)
DELETE FROM dupes WHERE rn > 1;

CREATE UNIQUE NONCLUSTERED INDEX [UQ_TaskParticipants_user_task_role]
    ON [TaskParticipants]([user_id] ASC, [task_id] ASC, [task_participant_role] ASC);

-- ============================================================
-- Blob-protection support columns + MaintenanceStatus table
-- ============================================================

-- 5. Rolling group-creation counter on Users
--    The daily maintenance job resets totalGroupsCreated to 0,
--    preventing delete-and-recreate abuse that piles up orphan blobs.

ALTER TABLE [Users]
    ADD [totalGroupsCreated]       INT NOT NULL DEFAULT 0;

ALTER TABLE [Users]
    ADD [groupCreationBudgetResetAt] DATETIME2 NULL;

-- 6. Singleton table that tracks the last successful maintenance run.
--    The backend reads nextResetAt to show a user-facing ETA when
--    a group-creation limit is hit.

CREATE TABLE [MaintenanceStatus] (
    [id]                BIGINT       NOT NULL,
    [lastBlobCleanupAt] DATETIME2    NULL,
    [lastFullCleanupAt] DATETIME2    NULL,
    [lastOrphanCount]   INT          NOT NULL DEFAULT 0,
    [lastBlobsScanned]  INT          NOT NULL DEFAULT 0,
    [nextResetAt]       DATETIME2    NULL,
    CONSTRAINT [PK_MaintenanceStatus] PRIMARY KEY ([id])
);
