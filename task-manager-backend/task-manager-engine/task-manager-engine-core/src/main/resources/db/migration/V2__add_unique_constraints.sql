
;WITH dupes AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY [azureKey] ORDER BY [id] ASC) AS rn
    FROM [Users]
)
DELETE FROM dupes WHERE rn > 1;

CREATE UNIQUE NONCLUSTERED INDEX [UQ_Users_azureKey]
    ON [Users]([azureKey] ASC);

;WITH dupes AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY [user_id], [group_id] ORDER BY [id] ASC) AS rn
    FROM [GroupMemberships]
)
DELETE FROM dupes WHERE rn > 1;

CREATE UNIQUE NONCLUSTERED INDEX [UQ_GroupMemberships_user_group]
    ON [GroupMemberships]([user_id] ASC, [group_id] ASC);
