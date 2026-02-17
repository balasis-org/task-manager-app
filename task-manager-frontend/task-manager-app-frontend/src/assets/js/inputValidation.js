/**
 * inputValidation.js
 *
 * Shared field-length constants (matching backend @Size / @Column limits)
 * and a lightweight sanitize helper for the UI layer.
 *
 * Import where needed:
 *   import { LIMITS, sanitize } from "@assets/js/inputValidation";
 */

/* ───────────── Field-length limits ───────────── */

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
});

/* ───────────── Invisible-character regex ───────────── */

// Unicode "invisible" whitespace codepoints that look blank but aren't
// ASCII 0x20. These are frequently used to bypass visual validation or
// to break copy-paste experiences.
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

/* ───────────── Sanitize helper ───────────── */

/**
 * Light client-side sanitisation:
 * 1. Replace invisible Unicode whitespace with normal space.
 * 2. Trim leading / trailing whitespace.
 *
 * Note: full XSS escaping is handled server-side (`InputSanitizer`).
 * This function focuses on UX — preventing confusing invisible
 * characters from creeping in via paste.
 *
 * @param {string} value
 * @returns {string}
 */
export function sanitize(value) {
    if (typeof value !== "string") return "";
    return value.replace(INVISIBLE_RE, " ").trim();
}
