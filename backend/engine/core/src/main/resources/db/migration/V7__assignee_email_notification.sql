
-- ============================================================
-- V7: Group-level toggle for assignee-to-reviewer email
--     notifications (opt-in, default OFF).
-- ============================================================

ALTER TABLE [Groups]
    ADD [allowAssigneeEmailNotification] BIT NOT NULL DEFAULT 0;
