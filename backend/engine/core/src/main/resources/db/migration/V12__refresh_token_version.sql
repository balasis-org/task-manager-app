-- add optimistic-locking version column for RefreshToken entity.
-- existing rows get version = 0 (Hibernate's @Version starting value).
ALTER TABLE [RefreshTokens] ADD [version] BIGINT NOT NULL DEFAULT 0;
