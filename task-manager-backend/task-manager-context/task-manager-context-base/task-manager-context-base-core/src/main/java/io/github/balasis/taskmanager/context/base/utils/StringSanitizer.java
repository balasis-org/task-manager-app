package io.github.balasis.taskmanager.context.base.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Small static sanitiser utilities for filenames and user-provided text.
 * - normalize Unicode
 * - remove control characters, CR/LF and path separators
 * - limit length
 * - provide safe fallbacks
 */
public final class StringSanitizer {

    private StringSanitizer() {}

    private static final int MAX_FILENAME_LENGTH = 50;
    private static final Pattern INVALID_FILENAME_PATTERN = Pattern.compile("[\\r\\n\\x00-\\x1F\\x7F/\\\\]+");


    public static String sanitizeFilename(String original) {
        if (original == null) return fallbackUuid();

        String normalized = Normalizer.normalize(original, Normalizer.Form.NFKC);
        String cleaned = INVALID_FILENAME_PATTERN.matcher(normalized).replaceAll("_");
        cleaned = cleaned.trim();
        if (cleaned.isEmpty()) return fallbackUuid();
        if (cleaned.length() > MAX_FILENAME_LENGTH) cleaned = cleaned.substring(0, MAX_FILENAME_LENGTH);
        return cleaned;
    }

    public static String sanitizeFilenameForHeader(String original) {
        return sanitizeFilename(original);
    }

    public static String toSafeBlobKey(Long prefixId, String originalFilename) {
        String safe = sanitizeFilename(originalFilename);
        String encoded = URLEncoder.encode(safe, StandardCharsets.UTF_8);
        if (prefixId == null) return encoded;
        return prefixId + "-" + encoded;
    }

//    public static String sanitizeTextPreserveUnicode(String input) {
//        if (input == null) return null;
//        // remove invisible/exotic spaces (keeps common whitespace)
//        String replaced = input.replaceAll("[\\u00A0\\u2000-\\u200B\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]", " ");
//        return replaced.trim();
//    }

    private static String fallbackUuid() {
        return UUID.randomUUID().toString();
    }
}
