package io.github.balasis.taskmanager.context.base.utils;

import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

// utility for sanitizing user-provided filenames before storing them as blob keys.
// prevents path traversal, null bytes, and other nasty chars from reaching Azure.
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

    // blob key format: {taskId}-{8charUUID}-{sanitized_filename}
    // the UUID segment prevents two uploads of the same filename from
    // silently overwriting each other in blob storage. without it, uploading
    // "report.pdf" twice to the same task would clobber the first file.
    public static String toSafeBlobKey(Long prefixId, String originalFilename) {
        String safe = sanitizeFilename(originalFilename);
        String blobSafe = BLOB_UNSAFE_PATTERN.matcher(safe).replaceAll("_");
        if (blobSafe.isEmpty()) blobSafe = fallbackUuid();
        // Include a short UUID segment to prevent overwrites when the same filename is uploaded twice
        String uniqueSegment = UUID.randomUUID().toString().substring(0, 8);
        if (prefixId == null) return uniqueSegment + "-" + blobSafe;
        return prefixId + "-" + uniqueSegment + "-" + blobSafe;
    }

    private static String fallbackUuid() {
        return UUID.randomUUID().toString();
    }
}
