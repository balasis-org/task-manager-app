// shared field-length limits (should match backend @Size / @Column)

/* limits */

export const LIMITS = Object.freeze({
    // Group
    GROUP_NAME:        50,
    GROUP_DESCRIPTION: 500,
    GROUP_ANNOUNCEMENT:150,

    // Task
    TASK_TITLE:        150,
    TASK_DESCRIPTION:  1500,
    TASK_REVIEW_COMMENT:400,

    // Comment
    COMMENT:           400,

    // Invitation
    INVITE_COMMENT:    400,

    // User
    USER_NAME:         100,

    // Files
    MAX_TASK_FILES:       3,   // creator files per task
    MAX_ASSIGNEE_FILES:   3,   // assignee files per task
    MAX_FILE_SIZE_MB:     20,  // max size per task file
    MAX_IMAGE_SIZE_MB:    5,   // max size per image upload
});
