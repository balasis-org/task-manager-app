
-- ============================================================
-- V5: Monthly download budget, email quota & repeat-download guard
-- ============================================================

-- 1. Monthly download-budget counter on Users
--    Tracks total bytes downloaded from groups the user owns.
--    Reset to 0 by maintenance at the start of each month.
ALTER TABLE [Users]
    ADD [usedDownloadBytesMonth] BIGINT NOT NULL DEFAULT 0;

-- 2. Monthly email-quota counter on Users
--    Tracks emails sent for groups the user owns (Organizer+ only).
--    Reset to 0 by maintenance at the start of each month.
ALTER TABLE [Users]
    ADD [usedEmailsMonth] INT NOT NULL DEFAULT 0;

-- 3. Per-group toggle: repeat-download guard
--    When enabled, limits how often a member can re-download the same file
--    in a 24-hour window. Defaults to ON (protecting the owner's budget).
ALTER TABLE [Groups]
    ADD [dailyDownloadCapEnabled] BIT NOT NULL DEFAULT 1;
