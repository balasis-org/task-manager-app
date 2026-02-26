-- V2__add_unique_constraints.sql
-- Adds missing unique constraints to prevent duplicate data.
-- Safely removes duplicates first (keeps the row with the lowest id).

-- ═══════════════════════════════════════════════════════════════
-- 1.  Users.azureKey  — must be unique (identity key from Azure AD)
-- ═══════════════════════════════════════════════════════════════

-- Remove duplicate azureKey rows, keeping the one with the smallest id
;WITH dupes AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY [azureKey] ORDER BY [id] ASC) AS rn
    FROM [Users]
)
DELETE FROM dupes WHERE rn > 1;

CREATE UNIQUE NONCLUSTERED INDEX [UQ_Users_azureKey]
    ON [Users]([azureKey] ASC);


-- ═══════════════════════════════════════════════════════════════
-- 2.  GroupMemberships(user_id, group_id) — one membership per user per group
-- ═══════════════════════════════════════════════════════════════

-- Remove duplicate memberships, keeping the one with the smallest id
;WITH dupes AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY [user_id], [group_id] ORDER BY [id] ASC) AS rn
    FROM [GroupMemberships]
)
DELETE FROM dupes WHERE rn > 1;

CREATE UNIQUE NONCLUSTERED INDEX [UQ_GroupMemberships_user_group]
    ON [GroupMemberships]([user_id] ASC, [group_id] ASC);
