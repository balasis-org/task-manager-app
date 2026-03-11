
-- ============================================================
-- V6: Monthly image-scan budget + Microsoft profile photo
-- ============================================================

-- Tracks how many Content Safety image scans the user has consumed
-- this calendar month (profile + group image uploads).
-- Reset to 0 by maintenance at the start of each month.
ALTER TABLE [Users]
    ADD [usedImageScansMonth] INT NOT NULL DEFAULT 0;

-- Stores the blob name of the user's Microsoft (Azure AD) profile
-- photo, fetched at login. NULL if the user has no Microsoft photo
-- or if the fetch was skipped / failed.
ALTER TABLE [Users]
    ADD [msProfilePhotoUrl] NVARCHAR(500) NULL;
