const ROLE_LABELS = {
    MEMBER: "Member",
    GUEST: "Guest",
    REVIEWER: "Reviewer",
    TASK_MANAGER: "Task Manager",
    GROUP_LEADER: "Group Leader",
};

export function formatRole(role) {
    return ROLE_LABELS[role] || role;
}
