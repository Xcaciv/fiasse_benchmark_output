package com.loosenotes.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Input canonicalization and sanitization utilities.
 * SSEM: Integrity - implements Canonicalize → Sanitize → Validate pipeline.
 * Trust boundary entry point: all user-supplied strings pass through here.
 *
 * <p>Stage 1 (canonicalize): Normalize Unicode form, trim whitespace.
 * Stage 2 (sanitize): Strip control characters, limit dangerous patterns.
 * Stage 3 (validate): Length and format checks (see ValidationUtil).
 */
public final class InputSanitizer {

    /** Matches Unicode control characters (except tab and newline). */
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cc}&&[^\\t\\n\\r]]");

    /** Matches null bytes (can bypass string checks in some parsers). */
    private static final Pattern NULL_BYTES = Pattern.compile("\\x00");

    private InputSanitizer() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Canonicalizes and sanitizes a single-line text input (title, username, etc.).
     * Strips newlines; trims whitespace; removes control characters.
     *
     * @param input raw user input (may be null)
     * @return sanitized string, or null if input was null
     */
    public static String sanitizeSingleLine(String input) {
        if (input == null) {
            return null;
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFC);
        String noNulls = NULL_BYTES.matcher(normalized).replaceAll("");
        String noControl = CONTROL_CHARS.matcher(noNulls).replaceAll("");
        return noControl.trim().replaceAll("\\s+", " ");
    }

    /**
     * Canonicalizes and sanitizes multi-line content (note body, comments).
     * Preserves newlines; removes null bytes and dangerous control chars.
     *
     * @param input raw user input (may be null)
     * @return sanitized string, or null if input was null
     */
    public static String sanitizeMultiLine(String input) {
        if (input == null) {
            return null;
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFC);
        String noNulls = NULL_BYTES.matcher(normalized).replaceAll("");
        return CONTROL_CHARS.matcher(noNulls).replaceAll("").trim();
    }

    /**
     * Sanitizes a filename by keeping only safe characters.
     * Returns a replacement name if the result would be empty.
     *
     * @param filename original filename from upload
     * @return sanitized filename safe for metadata storage
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed_file";
        }
        // Extract just the file name component (prevent path traversal via metadata)
        String name = filename.replaceAll(".*[/\\\\]", "");
        // Keep only safe characters
        String safe = name.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        return safe.isBlank() ? "unnamed_file" : safe;
    }

    /**
     * Normalizes an email address: lowercase, trim.
     *
     * @param email raw email input
     * @return canonicalized email, or null if input was null
     */
    public static String canonicalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    /**
     * Sanitizes a search query: trim, collapse whitespace, limit length.
     *
     * @param query raw search input
     * @param maxLength maximum allowed length
     * @return sanitized query safe for use in LIKE parameter
     */
    public static String sanitizeSearchQuery(String query, int maxLength) {
        if (query == null) {
            return "";
        }
        String normalized = Normalizer.normalize(query, Normalizer.Form.NFC);
        String cleaned = CONTROL_CHARS.matcher(normalized).replaceAll("").trim();
        // Escape LIKE special characters for safe parameterized use
        String escaped = cleaned.replace("\\", "\\\\")
                                .replace("%", "\\%")
                                .replace("_", "\\_");
        if (escaped.length() > maxLength) {
            escaped = escaped.substring(0, maxLength);
        }
        return escaped;
    }
}
