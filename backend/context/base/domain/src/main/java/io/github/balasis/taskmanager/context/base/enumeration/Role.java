package io.github.balasis.taskmanager.context.base.enumeration;

// group membership roles, ordered from least to most privileged.
// GUEST: read-only, can view tasks but cant modify anything
// MEMBER: can be assigned to tasks, upload assignee files, comment
// REVIEWER: can review files (CHECKED / NEEDS_REVISION), approve/reject tasks
// TASK_MANAGER: can create/edit/delete tasks, manage participants, upload creator files
// GROUP_LEADER: full control, can manage members, group settings, and invitations
// the permission matrix lives in RolePolicyService
public enum Role {
    GUEST,
    MEMBER,
    REVIEWER,
    TASK_MANAGER,
    GROUP_LEADER
}
