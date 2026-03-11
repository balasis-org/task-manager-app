package io.github.balasis.taskmanager.context.base.utils;

import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

public final class StringSanitizer {

    private StringSanitizer() {}

    private static final int MAX_FILENAME_LENGTH = 50;
    private static final Pattern INVALID_FILENAME_PATTERN = Pattern.compile("[\\r\\n\\x00-\\x1F\\x7F/\\\\]+");
    private static final Pattern BLOB_UNSAFE_PATTERN = Pattern.compile("[^a-zA-Z0-9._-]");

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
        String blobSafe = BLOB_UNSAFE_PATTERN.matcher(safe).replaceAll("_");
        if (blobSafe.isEmpty()) blobSafe = fallbackUuid();
        if (prefixId == null) return blobSafe;
        return prefixId + "-" + blobSafe;
    }

    private static String fallbackUuid() {
        return UUID.randomUUID().toString();
    }
}
