-- V11: Denormalized member-count column for atomic cap enforcement
-- ================================================================
-- Adds a memberCount column to Groups so max-members checks can be done
-- with an atomic UPDATE ... WHERE memberCount < :max instead of the
-- racy count-then-insert pattern (TOCTOU / ISSUE-024).

ALTER TABLE [Groups] ADD [memberCount] INT NOT NULL DEFAULT 0;

-- backfill from existing memberships
UPDATE g
SET    g.[memberCount] = sub.cnt
FROM   [Groups] g
JOIN   (SELECT [group_id], COUNT(*) AS cnt
        FROM   [GroupMemberships]
        GROUP BY [group_id]) sub
ON     g.[id] = sub.[group_id];
