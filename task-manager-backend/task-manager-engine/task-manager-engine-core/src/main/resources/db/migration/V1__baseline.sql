-- ===========================================================================
-- V1__baseline.sql
-- Flyway baseline migration — schema originally created by Hibernate ddl-auto.
-- Extracted from dev-mssql (SQL Server 2022) on 2026-02-19.
-- All future schema changes should be added as V2__, V3__, etc.
-- ===========================================================================

-- ===== TABLES =====

CREATE TABLE [Users] (
    [allowEmailNotification] bit NULL,
    [isOrg] bit NULL,
    [cacheKeyCreatedAt] datetimeoffset NULL,
    [id] bigint NOT NULL IDENTITY(1,1),
    [inviteCode] varchar(8) NULL,
    [inviteCodeCreatedAt] datetimeoffset NULL,
    [lastActiveAt] datetimeoffset NULL,
    [lastInviteReceivedAt] datetimeoffset NULL,
    [lastSeenInvites] datetimeoffset NULL,
    [systemRole] varchar(10) NULL,
    [subscriptionPlan] varchar(20) NULL,
    [cacheKey] varchar(64) NULL,
    [tenantId] varchar(64) NULL,
    [name] varchar(100) NOT NULL,
    [azureKey] varchar(128) NOT NULL,
    [email] varchar(254) NOT NULL,
    [defaultImgUrl] varchar(500) NULL,
    [imgUrl] varchar(500) NULL
);

CREATE TABLE [Groups] (
    [allowEmailNotification] bit NULL,
    [createdAt] datetimeoffset NULL,
    [id] bigint NOT NULL IDENTITY(1,1),
    [lastChangeInGroup] datetimeoffset NULL,
    [lastChangeInGroupNoJoins] datetimeoffset NULL,
    [lastDeleteTaskDate] datetimeoffset NULL,
    [lastGroupEventDate] datetimeoffset NULL,
    [lastMaintenanceDate] datetimeoffset NULL,
    [lastMemberChangeDate] datetimeoffset NULL,
    [owner_id] bigint NOT NULL,
    [name] varchar(50) NOT NULL,
    [Announcement] varchar(150) NULL,
    [defaultImgUrl] varchar(500) NULL,
    [description] varchar(500) NULL,
    [imgUrl] varchar(500) NULL
);

CREATE TABLE [default_images] (
    [id] bigint NOT NULL IDENTITY(1,1),
    [fileName] varchar(300) NULL,
    [type] varchar(255) NULL
);

CREATE TABLE [DeletedTasks] (
    [deletedAt] datetimeoffset NOT NULL,
    [deletedTaskId] bigint NULL,
    [group_id] bigint NOT NULL,
    [id] bigint NOT NULL IDENTITY(1,1)
);

CREATE TABLE [GroupEvent] (
    [createdAt] datetimeoffset NOT NULL,
    [group_id] bigint NOT NULL,
    [id] bigint NOT NULL IDENTITY(1,1),
    [description] varchar(MAX) NULL
);

CREATE TABLE [GroupInvitations] (
    [createdAt] datetimeoffset NOT NULL,
    [group_id] bigint NOT NULL,
    [id] bigint NOT NULL IDENTITY(1,1),
    [invited_by_id] bigint NOT NULL,
    [user_id] bigint NOT NULL,
    [comment] varchar(400) NULL,
    [invitationStatus] varchar(255) NOT NULL,
    [userToBeInvitedRole] varchar(255) NOT NULL
);

CREATE TABLE [GroupMemberships] (
    [group_id] bigint NOT NULL,
    [id] bigint NOT NULL IDENTITY(1,1),
    [lastSeenGroupEvents] datetimeoffset NULL,
    [user_id] bigint NOT NULL,
    [role] varchar(255) NOT NULL
);

CREATE TABLE [RefreshTokens] (
    [createdAt] datetimeoffset NULL,
    [expiresAt] datetimeoffset NULL,
    [id] bigint NOT NULL IDENTITY(1,1),
    [user_id] bigint NULL,
    [refreshCode] varchar(128) NOT NULL
);

CREATE TABLE [TaskAssigneeFiles] (
    [id] bigint NOT NULL IDENTITY(1,1),
    [task_id] bigint NULL,
    [fileUrl] varchar(500) NULL,
    [name] varchar(255) NULL
);

CREATE TABLE [TaskComments] (
    [createdAt] datetimeoffset NULL,
    [creator_id] bigint NULL,
    [id] bigint NOT NULL IDENTITY(1,1),
    [task_id] bigint NULL,
    [comment] nvarchar(800) NULL,
    [creatorNameSnapshot] varchar(255) NULL
);

CREATE TABLE [TaskFiles] (
    [id] bigint NOT NULL IDENTITY(1,1),
    [task_id] bigint NULL,
    [fileUrl] varchar(500) NULL,
    [name] varchar(255) NULL
);

CREATE TABLE [TaskParticipants] (
    [id] bigint NOT NULL IDENTITY(1,1),
    [lastSeenTaskComments] datetimeoffset NULL,
    [task_id] bigint NOT NULL,
    [user_id] bigint NOT NULL,
    [task_participant_role] varchar(255) NOT NULL
);

CREATE TABLE [Tasks] (
    [priority] int NULL,
    [commentCount] bigint NULL,
    [createdAt] datetimeoffset NULL,
    [creator_id_snapshot] bigint NULL,
    [dueDate] datetimeoffset NULL,
    [group_id] bigint NOT NULL,
    [id] bigint NOT NULL IDENTITY(1,1),
    [lastChangeDate] datetimeoffset NULL,
    [lastChangeDateInComments] datetimeoffset NULL,
    [lastChangeDateInParticipants] datetimeoffset NULL,
    [lastChangeDateNoJoins] datetimeoffset NULL,
    [lastCommentDate] datetimeoffset NULL,
    [lastEditBy_id] bigint NULL,
    [lastEditDate] datetimeoffset NULL,
    [reviewedBy_id] bigint NULL,
    [creator_name_snapshot] varchar(100) NULL,
    [title] varchar(150) NOT NULL,
    [reviewersDecision] varchar(255) NULL,
    [taskState] varchar(255) NOT NULL,
    [description] varchar(MAX) NOT NULL,
    [reviewComment] varchar(MAX) NULL
);

-- ===== PRIMARY KEYS =====

ALTER TABLE [Users] ADD CONSTRAINT [PK_Users] PRIMARY KEY ([id]);
ALTER TABLE [Groups] ADD CONSTRAINT [PK_Groups] PRIMARY KEY ([id]);
ALTER TABLE [default_images] ADD CONSTRAINT [PK_default_images] PRIMARY KEY ([id]);
ALTER TABLE [DeletedTasks] ADD CONSTRAINT [PK_DeletedTasks] PRIMARY KEY ([id]);
ALTER TABLE [GroupEvent] ADD CONSTRAINT [PK_GroupEvent] PRIMARY KEY ([id]);
ALTER TABLE [GroupInvitations] ADD CONSTRAINT [PK_GroupInvitations] PRIMARY KEY ([id]);
ALTER TABLE [GroupMemberships] ADD CONSTRAINT [PK_GroupMemberships] PRIMARY KEY ([id]);
ALTER TABLE [RefreshTokens] ADD CONSTRAINT [PK_RefreshTokens] PRIMARY KEY ([id]);
ALTER TABLE [TaskAssigneeFiles] ADD CONSTRAINT [PK_TaskAssigneeFiles] PRIMARY KEY ([id]);
ALTER TABLE [TaskComments] ADD CONSTRAINT [PK_TaskComments] PRIMARY KEY ([id]);
ALTER TABLE [TaskFiles] ADD CONSTRAINT [PK_TaskFiles] PRIMARY KEY ([id]);
ALTER TABLE [TaskParticipants] ADD CONSTRAINT [PK_TaskParticipants] PRIMARY KEY ([id]);
ALTER TABLE [Tasks] ADD CONSTRAINT [PK_Tasks] PRIMARY KEY ([id]);

-- ===== UNIQUE CONSTRAINTS =====

ALTER TABLE [Users] ADD CONSTRAINT [UQ_Users_azureKey] UNIQUE ([azureKey]);
ALTER TABLE [Tasks] ADD CONSTRAINT [UQ_Tasks_title] UNIQUE ([title]);
ALTER TABLE [Groups] ADD CONSTRAINT [UQ_Groups_name_owner] UNIQUE ([name], [owner_id]);
ALTER TABLE [GroupMemberships] ADD CONSTRAINT [UQ_GroupMemberships_user_group] UNIQUE ([user_id], [group_id]);
ALTER TABLE [TaskParticipants] ADD CONSTRAINT [UQ_TaskParticipants_user_task_role] UNIQUE ([user_id], [task_id], [task_participant_role]);

-- ===== FOREIGN KEYS =====

ALTER TABLE [Groups] ADD CONSTRAINT [FK_Groups_owner] FOREIGN KEY ([owner_id]) REFERENCES [Users] ([id]);
ALTER TABLE [DeletedTasks] ADD CONSTRAINT [FK_DeletedTasks_group] FOREIGN KEY ([group_id]) REFERENCES [Groups] ([id]);
ALTER TABLE [GroupEvent] ADD CONSTRAINT [FK_GroupEvent_group] FOREIGN KEY ([group_id]) REFERENCES [Groups] ([id]);
ALTER TABLE [GroupInvitations] ADD CONSTRAINT [FK_GroupInvitations_user] FOREIGN KEY ([user_id]) REFERENCES [Users] ([id]);
ALTER TABLE [GroupInvitations] ADD CONSTRAINT [FK_GroupInvitations_invitedBy] FOREIGN KEY ([invited_by_id]) REFERENCES [Users] ([id]);
ALTER TABLE [GroupInvitations] ADD CONSTRAINT [FK_GroupInvitations_group] FOREIGN KEY ([group_id]) REFERENCES [Groups] ([id]);
ALTER TABLE [GroupMemberships] ADD CONSTRAINT [FK_GroupMemberships_group] FOREIGN KEY ([group_id]) REFERENCES [Groups] ([id]);
ALTER TABLE [GroupMemberships] ADD CONSTRAINT [FK_GroupMemberships_user] FOREIGN KEY ([user_id]) REFERENCES [Users] ([id]);
ALTER TABLE [RefreshTokens] ADD CONSTRAINT [FK_RefreshTokens_user] FOREIGN KEY ([user_id]) REFERENCES [Users] ([id]);
ALTER TABLE [TaskAssigneeFiles] ADD CONSTRAINT [FK_TaskAssigneeFiles_task] FOREIGN KEY ([task_id]) REFERENCES [Tasks] ([id]);
ALTER TABLE [TaskComments] ADD CONSTRAINT [FK_TaskComments_creator] FOREIGN KEY ([creator_id]) REFERENCES [Users] ([id]);
ALTER TABLE [TaskComments] ADD CONSTRAINT [FK_TaskComments_task] FOREIGN KEY ([task_id]) REFERENCES [Tasks] ([id]);
ALTER TABLE [TaskFiles] ADD CONSTRAINT [FK_TaskFiles_task] FOREIGN KEY ([task_id]) REFERENCES [Tasks] ([id]);
ALTER TABLE [TaskParticipants] ADD CONSTRAINT [FK_TaskParticipants_task] FOREIGN KEY ([task_id]) REFERENCES [Tasks] ([id]);
ALTER TABLE [TaskParticipants] ADD CONSTRAINT [FK_TaskParticipants_user] FOREIGN KEY ([user_id]) REFERENCES [Users] ([id]);
ALTER TABLE [Tasks] ADD CONSTRAINT [FK_Tasks_group] FOREIGN KEY ([group_id]) REFERENCES [Groups] ([id]);
ALTER TABLE [Tasks] ADD CONSTRAINT [FK_Tasks_lastEditBy] FOREIGN KEY ([lastEditBy_id]) REFERENCES [Users] ([id]);
ALTER TABLE [Tasks] ADD CONSTRAINT [FK_Tasks_reviewedBy] FOREIGN KEY ([reviewedBy_id]) REFERENCES [Users] ([id]);

-- ===== INDEXES =====

CREATE INDEX [idx_di_type] ON [default_images] ([type] ASC);
CREATE INDEX [idx_dt_group_deleted] ON [DeletedTasks] ([group_id] ASC, [deletedAt] ASC);
CREATE UNIQUE INDEX [UQ_DeletedTasks_group_taskId] ON [DeletedTasks] ([group_id] ASC, [deletedTaskId] ASC);
CREATE INDEX [idx_ge_group] ON [GroupEvent] ([group_id] ASC);
CREATE INDEX [idx_gi_group] ON [GroupInvitations] ([group_id] ASC);
CREATE INDEX [idx_gi_invited_by] ON [GroupInvitations] ([invited_by_id] ASC);
CREATE INDEX [idx_gi_user_status] ON [GroupInvitations] ([user_id] ASC, [invitationStatus] ASC);
CREATE INDEX [idx_gm_group] ON [GroupMemberships] ([group_id] ASC);
CREATE INDEX [idx_rt_user] ON [RefreshTokens] ([user_id] ASC);
CREATE INDEX [idx_taf_task] ON [TaskAssigneeFiles] ([task_id] ASC);
CREATE INDEX [idx_tc_creator] ON [TaskComments] ([creator_id] ASC);
CREATE INDEX [idx_tc_task] ON [TaskComments] ([task_id] ASC);
CREATE INDEX [idx_tf_task] ON [TaskFiles] ([task_id] ASC);
CREATE INDEX [idx_tp_task] ON [TaskParticipants] ([task_id] ASC);
CREATE INDEX [idx_task_group] ON [Tasks] ([group_id] ASC);
CREATE INDEX [idx_task_group_lastchange] ON [Tasks] ([group_id] ASC, [lastChangeDate] ASC);
CREATE INDEX [idx_user_email] ON [Users] ([email] ASC);
CREATE UNIQUE INDEX [UQ_Users_inviteCode] ON [Users] ([inviteCode] ASC);
