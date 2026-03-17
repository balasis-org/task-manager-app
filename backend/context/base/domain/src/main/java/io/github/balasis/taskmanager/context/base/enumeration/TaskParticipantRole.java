package io.github.balasis.taskmanager.context.base.enumeration;

// role of a user within a specific task (not group-level, thats Role.java).
// CREATOR: the user who made the task (or a task manager who took ownership)
// ASSIGNEE: the user assigned to actually do the work
// REVIEWER: the user who reviews the assignee's submissions
public enum TaskParticipantRole {
    ASSIGNEE,
    REVIEWER,
    CREATOR
}
