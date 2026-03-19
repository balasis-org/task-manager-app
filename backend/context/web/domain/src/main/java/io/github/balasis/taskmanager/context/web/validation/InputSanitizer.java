package io.github.balasis.taskmanager.context.web.validation;

// strips invisible / zero-width Unicode characters (NBSP, ZWSP, etc.) and trims.
// used by SanitizingRequestBodyAdvice on every inbound String field.
public final class InputSanitizer {

    private InputSanitizer() {}

    private static final String INVISIBLE_REGEX =
            "[\\u00A0\\u2000-\\u200B\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]";

    public static String sanitize(String input) {
        if (input == null) return null;
        String clean = input.replaceAll(INVISIBLE_REGEX, " ");
        clean = clean.trim();
        return clean;
    }
}
