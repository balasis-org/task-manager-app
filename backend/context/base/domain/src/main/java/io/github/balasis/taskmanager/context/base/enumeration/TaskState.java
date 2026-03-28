package io.github.balasis.taskmanager.context.base.enumeration;

// the task lifecycle state machine.
// dedicated transition endpoints in GroupServiceImpl enforce the flow:
//   TODO -> IN_PROGRESS (assignee starts working)
//   IN_PROGRESS -> TO_BE_REVIEWED (assignee marks ready for review)
//   TO_BE_REVIEWED -> DONE (reviewer approves)
//   TO_BE_REVIEWED -> IN_PROGRESS (reviewer rejects, sends back)
//   DONE -> IN_PROGRESS (reopen)
// patchTask (GROUP_LEADER / TASK_MANAGER only) allows any transition as an
// override — this is intentional for leader flexibility.
public enum TaskState {
    TODO,
    IN_PROGRESS,
    TO_BE_REVIEWED,
    DONE
}
