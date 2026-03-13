package io.github.balasis.taskmanager.engine.core.test;

import io.github.balasis.taskmanager.context.base.utils.StringSanitizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringSanitizerTest {

    // ── sanitizeFilename ────────────────────────────────────────

    @Test
    void sanitizeFilename_nullInput_returnsFallbackUuid() {
        String result = SanitizeFilename(null);
        assertIsUuid(result);
    }

    @Test
    void sanitizeFilename_emptyString_returnsFallbackUuid() {
        String result = SanitizeFilename("");
        assertIsUuid(result);
    }

    @Test
    void sanitizeFilename_whitespaceOnly_returnsFallbackUuid() {
        String result = SanitizeFilename("     ");
        assertIsUuid(result);
    }

    @Test
    void sanitizeFilename_normalName_remainsUnchanged() {
        String result = SanitizeFilename("report.pdf");
        assertEquals("report.pdf", result);
    }

    @Test
    void sanitizeFilename_pathTraversal_stripsSlashes() {
        String result = SanitizeFilename("../../etc/passwd");
        assertFalse(result.contains("/"), "forward slashes should be stripped");
        assertFalse(result.contains("\\"), "backslashes should be stripped");
    }

    @Test
    void sanitizeFilename_controlCharacters_replacedWithUnderscore() {
        String result = SanitizeFilename("file\u0000name\r\n.txt");
        assertFalse(result.contains("\u0000"));
        assertFalse(result.contains("\r"));
        assertFalse(result.contains("\n"));
    }

    @Test
    void sanitizeFilename_longName_truncatedTo50Characters() {
        String longName = "a".repeat(100) + ".pdf";
        String result = SanitizeFilename(longName);
        assertTrue(result.length() <= 50, "should be at most 50 characters");
    }

    @Test
    void sanitizeFilename_unicodeInput_normalizedWithNfkc() {
        // "ﬁle" uses the fi ligature (U+FB01), NFKC normalizes it to "fi"
        String result = SanitizeFilename("\uFB01le.txt");
        assertEquals("file.txt", result);
    }

    // ── sanitizeFilenameForHeader ───────────────────────────────

    @Test
    void sanitizeFilenameForHeader_delegatesToSanitizeFilename() {
        String direct = StringSanitizer.sanitizeFilename("test.pdf");
        String header = StringSanitizer.sanitizeFilenameForHeader("test.pdf");
        assertEquals(direct, header);
    }

    // ── toSafeBlobKey ───────────────────────────────────────────

    @Test
    void toSafeBlobKey_withPrefixId_prependsIdDash() {
        String result = SafeBlobKey(42L, "image.png");
        assertTrue(result.startsWith("42-"), "should start with prefix id");
    }

    @Test
    void toSafeBlobKey_withNullPrefixId_noPrefixAdded() {
        String result = SafeBlobKey(null, "image.png");
        assertFalse(result.contains("-") && result.startsWith("null"),
                "should not prepend 'null'");
    }

    @Test
    void toSafeBlobKey_specialCharacters_replacedWithUnderscore() {
        String result = SafeBlobKey(1L, "my file (1).jpg");
        assertFalse(result.contains(" "), "spaces should be replaced");
        assertFalse(result.contains("("), "parentheses should be replaced");
    }

    @Test
    void toSafeBlobKey_nullFilename_returnsFallbackUuid() {
        String result = SafeBlobKey(1L, null);
        // prefixId + "-" + uuid
        assertTrue(result.startsWith("1-"), "should still have prefix");
    }

    // ── private helpers ─────────────────────────────────────────

    private String SanitizeFilename(String input) {
        return StringSanitizer.sanitizeFilename(input);
    }

    private String SafeBlobKey(Long prefixId, String filename) {
        return StringSanitizer.toSafeBlobKey(prefixId, filename);
    }

    private void assertIsUuid(String value) {
        assertNotNull(value, "fallback should not be null");
        // UUID format: 8-4-4-4-12 hex chars
        assertTrue(value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "expected UUID format but got: " + value);
    }
}
