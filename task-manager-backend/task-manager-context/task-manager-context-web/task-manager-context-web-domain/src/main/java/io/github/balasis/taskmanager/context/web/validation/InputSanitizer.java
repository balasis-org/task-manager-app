package io.github.balasis.taskmanager.context.web.validation;

import org.springframework.web.util.HtmlUtils;

/**
 * Simple HTML / XSS sanitizer for user-provided strings.
 * <p>
 * being alergic to fake spaces ==
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
     *   <li>HTML-escape any remaining markup (&amp; &lt; &gt; &quot; &#39;).</li>
     * </ol>
     */
    public static String sanitize(String input) {
        if (input == null) return null;
        String clean = input.replaceAll(INVISIBLE_REGEX, " ");
        clean = clean.trim();
        clean = HtmlUtils.htmlEscape(clean);
        return clean;
    }
}
