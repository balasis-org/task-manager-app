package io.github.balasis.taskmanager.context.web.validation;

/**
 * Input sanitizer for user-provided strings.
 * <p>
 * Handles invisible/exotic Unicode whitespace that can bypass visual inspection.
 * <b>HTML/XSS escaping is NOT performed here</b> — it is the responsibility
 * of the rendering layer (React's JSX auto-escaping handles this for the frontend).
 * Storing raw text avoids double-escaping issues across consumers.
 */
public final class InputSanitizer {

    private InputSanitizer() {} // utility class

    // Unicode whitespace / invisible characters that look like spaces but aren't ASCII 0x20
    // \u00A0  No-Break Space
    // \u2000–\u200B  various typographic spaces + zero-width space
    // \u2028  Line Separator
    // \u2029  Paragraph Separator
    // \u202F  Narrow No-Break Space
    // \u205F  Medium Mathematical Space
    // \u3000  Ideographic Space
    // \uFEFF  BOM / Zero Width No-Break Space
    // NOTE: \u200C (ZWNJ) and \u200D (ZWJ) are intentionally EXCLUDED —
    //       they are required for compound emoji sequences (skin-tones, families, flags).
    private static final String INVISIBLE_REGEX =
            "[\\u00A0\\u2000-\\u200B\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]";

    /**
     * Sanitise a user-provided string:
     * <ol>
     *   <li>Return null unchanged (so optional fields stay null).</li>
     *   <li>Replace invisible/exotic Unicode whitespace with regular space.</li>
     *   <li>Trim leading/trailing whitespace.</li>
     * </ol>
     * Greek, Cyrillic, CJK and other non-ASCII scripts pass through unmodified.
     */
    public static String sanitize(String input) {
        if (input == null) return null;
        String clean = input.replaceAll(INVISIBLE_REGEX, " ");
        clean = clean.trim();
        return clean;
    }
}
