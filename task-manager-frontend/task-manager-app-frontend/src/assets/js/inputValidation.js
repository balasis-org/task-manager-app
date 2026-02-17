// shared field-length limits (should match backend @Size / @Column)
// and a basic sanitize helper for the UI.

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
    MAX_FILE_SIZE_MB:     40,  // max size per task file
    MAX_IMAGE_SIZE_MB:    5,   // max size per image upload
});

/* invisible-char regex
   unicode "invisible" whitespace codepoints that look blank but arent ASCII 0x20.
   used to bypass visual validation or break copy-paste. */
//
// \u00A0  No-Break Space
// \u2000–\u200B  various typographic spaces + zero-width space
// \u200C  Zero Width Non-Joiner
// \u200D  Zero Width Joiner
// \u2028  Line Separator
// \u2029  Paragraph Separator
// \u202F  Narrow No-Break Space
// \u205F  Medium Mathematical Space
// \u3000  Ideographic Space
// \uFEFF  BOM / Zero Width No-Break Space
const INVISIBLE_RE =
    /[\u00A0\u2000-\u200B\u200C\u200D\u2028\u2029\u202F\u205F\u3000\uFEFF]/g;

/* Sanitize helper */

// replaces invisible unicode whitespace with normal space, then trims.
// full XSS escaping happens server-side (InputSanitizer),
// this is just to prevent weird invisible chars from pasting.
export function sanitize(value) {
    if (typeof value !== "string") return "";
    return value.replace(INVISIBLE_RE, " ").trim();
}
