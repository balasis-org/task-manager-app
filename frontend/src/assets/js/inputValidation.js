
// text-length limits — these never vary by plan
export const LIMITS = Object.freeze({

    GROUP_NAME:        50,
    GROUP_DESCRIPTION: 500,
    GROUP_ANNOUNCEMENT:150,

    TASK_TITLE:        150,
    TASK_DESCRIPTION:  1500,
    TASK_REVIEW_COMMENT:400,

    COMMENT:           400,

    INVITE_COMMENT:    400,

    EMAIL_CUSTOM_NOTE: 300,

    USER_NAME:         100,

    // image uploads (avatar / group picture) are not plan-dependent
    MAX_IMAGE_SIZE_MB:    5,
});

// safe fallbacks for file limits when groupDetail hasn't loaded yet
const FILE_LIMIT_DEFAULTS = Object.freeze({
    maxCreatorFiles:   1,
    maxAssigneeFiles:  2,
    maxFileSizeBytes:  5 * 1024 * 1024,   // 5 MB (FREE plan)
    maxTasks:          30,
    maxMembers:        8,
});

/**
 * Extracts file/task limits from a GroupWithPreviewDto, falling back to
 * conservative FREE-tier defaults when the detail hasn't loaded yet.
 */
export function groupFileLimits(groupDetail) {
    return {
        maxCreatorFiles:  groupDetail?.mcf  ?? FILE_LIMIT_DEFAULTS.maxCreatorFiles,
        maxAssigneeFiles: groupDetail?.maf  ?? FILE_LIMIT_DEFAULTS.maxAssigneeFiles,
        maxFileSizeBytes: groupDetail?.mfsb ?? FILE_LIMIT_DEFAULTS.maxFileSizeBytes,
        maxTasks:         groupDetail?.mt   ?? FILE_LIMIT_DEFAULTS.maxTasks,
        maxMembers:       groupDetail?.mm   ?? FILE_LIMIT_DEFAULTS.maxMembers,
    };
}
