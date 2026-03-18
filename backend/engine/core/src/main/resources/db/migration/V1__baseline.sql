-- V1: Baseline schema — all core tables (Users, Groups, Tasks, TaskFiles,
-- GroupMemberships, GroupInvitations, Comments, GroupEvent, DeletedTasks,
-- default_images, RefreshTokens, MaintenanceStatus). Created from
-- Hibernate's ddl-auto=create output and normalized.

CREATE TABLE [default_images](
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [fileName] NVARCHAR(300) NULL,
    [type] NVARCHAR(255) NULL,
    CONSTRAINT PK_default_images PRIMARY KEY ([id])
    );

CREATE TABLE [DeletedTasks](
    [deletedAt] DATETIMEOFFSET(6) NOT NULL,
    [deletedTaskId] BIGINT NULL,
    [group_id] BIGINT NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    CONSTRAINT PK_DeletedTasks PRIMARY KEY ([id])
    );

CREATE TABLE [GroupEvent](
    [createdAt] DATETIMEOFFSET(6) NOT NULL,
    [group_id] BIGINT NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [description] NVARCHAR(MAX) NULL,
    CONSTRAINT PK_GroupEvent PRIMARY KEY ([id])
    );

CREATE TABLE [GroupInvitations](
    [createdAt] DATETIMEOFFSET(6) NOT NULL,
    [group_id] BIGINT NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [invited_by_id] BIGINT NOT NULL,
    [user_id] BIGINT NOT NULL,
    [comment] NVARCHAR(400) NULL,
    [invitationStatus] NVARCHAR(255) NOT NULL,
    [userToBeInvitedRole] NVARCHAR(255) NOT NULL,
    CONSTRAINT PK_GroupInvitations PRIMARY KEY ([id])
    );

CREATE TABLE [GroupMemberships](
    [group_id] BIGINT NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [lastSeenGroupEvents] DATETIMEOFFSET(6) NULL,
    [user_id] BIGINT NOT NULL,
    [role] NVARCHAR(255) NOT NULL,
    CONSTRAINT PK_GroupMemberships PRIMARY KEY ([id])
    );

CREATE TABLE [Groups](
    [allowEmailNotification] BIT NULL,
    [createdAt] DATETIMEOFFSET(6) NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [lastChangeInGroup] DATETIMEOFFSET(6) NULL,
    [lastChangeInGroupNoJoins] DATETIMEOFFSET(6) NULL,
    [lastDeleteTaskDate] DATETIMEOFFSET(6) NULL,
    [lastGroupEventDate] DATETIMEOFFSET(6) NULL,
    [lastMaintenanceDate] DATETIMEOFFSET(6) NULL,
    [lastMemberChangeDate] DATETIMEOFFSET(6) NULL,
    [owner_id] BIGINT NOT NULL,
    [name] NVARCHAR(50) NOT NULL,
    [Announcement] NVARCHAR(150) NULL,
    [defaultImgUrl] NVARCHAR(500) NULL,
    [description] NVARCHAR(500) NULL,
    [imgUrl] NVARCHAR(500) NULL,
    CONSTRAINT PK_Groups PRIMARY KEY ([id])
    );

CREATE TABLE [RefreshTokens](
    [createdAt] DATETIMEOFFSET(6) NULL,
    [expiresAt] DATETIMEOFFSET(6) NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [user_id] BIGINT NULL,
    [refreshCode] NVARCHAR(128) NOT NULL,
    CONSTRAINT PK_RefreshTokens PRIMARY KEY ([id])
    );

CREATE TABLE [TaskAssigneeFiles](
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [task_id] BIGINT NULL,
    [fileUrl] NVARCHAR(500) NULL,
    [name] NVARCHAR(255) NULL,
    CONSTRAINT PK_TaskAssigneeFiles PRIMARY KEY ([id])
    );

CREATE TABLE [TaskComments](
    [createdAt] DATETIMEOFFSET(6) NULL,
    [creator_id] BIGINT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [task_id] BIGINT NULL,
    [comment] NVARCHAR(800) NULL,
    [creatorNameSnapshot] NVARCHAR(255) NULL,
    CONSTRAINT PK_TaskComments PRIMARY KEY ([id])
    );

CREATE TABLE [TaskFiles](
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [task_id] BIGINT NULL,
    [fileUrl] NVARCHAR(500) NULL,
    [name] NVARCHAR(255) NULL,
    CONSTRAINT PK_TaskFiles PRIMARY KEY ([id])
    );

CREATE TABLE [TaskParticipants](
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [lastSeenTaskComments] DATETIMEOFFSET(6) NULL,
    [task_id] BIGINT NOT NULL,
    [user_id] BIGINT NOT NULL,
    [task_participant_role] NVARCHAR(255) NOT NULL,
    CONSTRAINT PK_TaskParticipants PRIMARY KEY ([id])
    );

CREATE TABLE [Tasks](
    [priority] INT NULL,
    [commentCount] BIGINT NULL,
    [createdAt] DATETIMEOFFSET(6) NULL,
    [creator_id_snapshot] BIGINT NULL,
    [dueDate] DATETIMEOFFSET(6) NULL,
    [group_id] BIGINT NOT NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [lastChangeDate] DATETIMEOFFSET(6) NULL,
    [lastChangeDateInComments] DATETIMEOFFSET(6) NULL,
    [lastChangeDateInParticipants] DATETIMEOFFSET(6) NULL,
    [lastChangeDateNoJoins] DATETIMEOFFSET(6) NULL,
    [lastCommentDate] DATETIMEOFFSET(6) NULL,
    [lastEditBy_id] BIGINT NULL,
    [lastEditDate] DATETIMEOFFSET(6) NULL,
    [reviewedBy_id] BIGINT NULL,
    [creator_name_snapshot] NVARCHAR(100) NULL,
    [title] NVARCHAR(150) NOT NULL,
    [reviewersDecision] NVARCHAR(255) NULL,
    [taskState] NVARCHAR(255) NOT NULL,
    [description] NVARCHAR(MAX) NOT NULL,
    [reviewComment] NVARCHAR(MAX) NULL,
    CONSTRAINT PK_Tasks PRIMARY KEY ([id])
    );

CREATE TABLE [Users](
    [allowEmailNotification] BIT NULL,
    [isOrg] BIT NULL,
    [cacheKeyCreatedAt] DATETIMEOFFSET(6) NULL,
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [inviteCode] NVARCHAR(8) NULL,
    [inviteCodeCreatedAt] DATETIMEOFFSET(6) NULL,
    [lastActiveAt] DATETIMEOFFSET(6) NULL,
    [lastInviteReceivedAt] DATETIMEOFFSET(6) NULL,
    [lastSeenInvites] DATETIMEOFFSET(6) NULL,
    [systemRole] NVARCHAR(10) NULL,
    [subscriptionPlan] NVARCHAR(20) NULL,
    [cacheKey] NVARCHAR(64) NULL,
    [tenantId] NVARCHAR(64) NULL,
    [name] NVARCHAR(100) NOT NULL,
    [azureKey] NVARCHAR(128) NOT NULL,
    [email] NVARCHAR(254) NOT NULL,
    [defaultImgUrl] NVARCHAR(500) NULL,
    [imgUrl] NVARCHAR(500) NULL,
    CONSTRAINT PK_Users PRIMARY KEY ([id])
    );

CREATE NONCLUSTERED INDEX [idx_di_type] ON [default_images] ([type] ASC);
CREATE NONCLUSTERED INDEX [idx_dt_group_deleted] ON [DeletedTasks] ([group_id] ASC, [deletedAt] ASC);
CREATE UNIQUE NONCLUSTERED INDEX [UKrwlk3ited1kvno38urknlwk02] ON [DeletedTasks]([group_id] ASC,[deletedTaskId] ASC)
    WHERE ([group_id] IS NOT NULL AND [deletedTaskId] IS NOT NULL);
CREATE NONCLUSTERED INDEX [idx_ge_group] ON [GroupEvent] ([group_id] ASC);
CREATE NONCLUSTERED INDEX [idx_gi_group] ON [GroupInvitations]([group_id] ASC);
CREATE NONCLUSTERED INDEX [idx_gi_invited_by] ON [GroupInvitations]([invited_by_id] ASC);
CREATE NONCLUSTERED INDEX [idx_gi_user_status] ON [GroupInvitations]([user_id] ASC,[invitationStatus] ASC);
CREATE NONCLUSTERED INDEX [idx_gm_group] ON [GroupMemberships]([group_id] ASC);
CREATE NONCLUSTERED INDEX [idx_rt_user] ON [RefreshTokens]([user_id] ASC);
CREATE NONCLUSTERED INDEX [idx_taf_task] ON [TaskAssigneeFiles]([task_id] ASC);
CREATE NONCLUSTERED INDEX [idx_tc_creator] ON [TaskComments]([creator_id] ASC);
CREATE NONCLUSTERED INDEX [idx_tc_task] ON [TaskComments]([task_id] ASC);
CREATE NONCLUSTERED INDEX [idx_tf_task] ON [TaskFiles]([task_id] ASC);
CREATE NONCLUSTERED INDEX [idx_tp_task] ON [TaskParticipants]([task_id] ASC);
CREATE NONCLUSTERED INDEX [idx_task_group] ON [Tasks]([group_id] ASC);
CREATE NONCLUSTERED INDEX [idx_task_group_lastchange] ON [Tasks]([group_id] ASC,[lastChangeDate] ASC);
CREATE NONCLUSTERED INDEX [idx_user_email] ON [Users]([email] ASC);
CREATE UNIQUE NONCLUSTERED INDEX [UKefj2sfg983kmiwhkwra4f4w3x] ON [Users]([inviteCode] ASC)
    WHERE ([inviteCode] IS NOT NULL);

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

ALTER TABLE [default_images]  WITH CHECK ADD CONSTRAINT CK_default_images_type CHECK ([type] IN ('GROUP_IMAGES','PROFILE_IMAGES','TASK_FILES','TASK_ASSIGNEE_FILES'));
ALTER TABLE [GroupInvitations]  WITH CHECK ADD CONSTRAINT CK_GroupInvitations_status CHECK ([invitationStatus] IN ('DECLINED','ACCEPTED','PENDING'));
ALTER TABLE [GroupInvitations]  WITH CHECK ADD CONSTRAINT CK_GroupInvitations_role CHECK ([userToBeInvitedRole] IN ('GROUP_LEADER','TASK_MANAGER','REVIEWER','MEMBER','GUEST'));
ALTER TABLE [GroupMemberships]  WITH CHECK ADD CONSTRAINT CK_GroupMemberships_role CHECK ([role] IN ('GROUP_LEADER','TASK_MANAGER','REVIEWER','MEMBER','GUEST'));
ALTER TABLE [TaskParticipants]  WITH CHECK ADD CONSTRAINT CK_TaskParticipants_role CHECK ([task_participant_role] IN ('CREATOR','REVIEWER','ASSIGNEE'));
ALTER TABLE [Tasks]  WITH CHECK ADD CONSTRAINT CK_Tasks_reviewersDecision CHECK ([reviewersDecision] IN ('APPROVE','REJECT'));
ALTER TABLE [Tasks]  WITH CHECK ADD CONSTRAINT CK_Tasks_taskState CHECK ([taskState] IN ('DONE','TO_BE_REVIEWED','IN_PROGRESS','TODO'));
ALTER TABLE [Users]  WITH CHECK ADD CONSTRAINT CK_Users_subscriptionPlan CHECK ([subscriptionPlan] IN ('PREMIUM','FREE'));
ALTER TABLE [Users]  WITH CHECK ADD CONSTRAINT CK_Users_systemRole CHECK ([systemRole] IN ('ADMIN','USER'));
