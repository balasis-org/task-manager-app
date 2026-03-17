package io.github.balasis.taskmanager.context.base.enumeration;

// the task lifecycle state machine.
// allowed transitions are enforced in GroupServiceImpl:
//   TODO -> IN_PROGRESS (assignee starts working)
//   IN_PROGRESS -> TO_BE_REVIEWED (assignee marks ready for review)
//   TO_BE_REVIEWED -> DONE (reviewer approves)
//   TO_BE_REVIEWED -> IN_PROGRESS (reviewer rejects, sends back)
//   DONE -> IN_PROGRESS (reopen)
// any other transition is rejected.
public enum TaskState {
    TODO,
    IN_PROGRESS,
    TO_BE_REVIEWED,
    DONE
}
