package com.loosenotes.util;

/**
 * Input sanitization utilities (canonicalize → sanitize step).
 *
 * SSEM / ASVS alignment:
 * - ASVS V2.2 (Input Sanitization): strip null bytes, control characters.
 * - S6.4.1 (Canonical Input Handling): applied before validation.
 * - Integrity: removes characters that could cause downstream issues.
 *
 * NOTE: HTML encoding of output is handled in JSP via JSTL {@code <c:out>}
 * and EL auto-escaping. This class handles *input* sanitization only.
 */
public final class InputSanitizer {

    private InputSanitizer() {}

    /**
     * Strips null bytes and ASCII control characters (except newline/tab/CR)
     * from the input. Returns null if the input is null.
     *
     * @param input the raw user-supplied string
     * @return sanitized string or null
     */
    public static String sanitize(String input) {
        if (input == null) return null;
        // Remove null bytes and most control characters; allow tab, LF, CR
        return input.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
    }

    /**
     * Sanitizes a single-line field (removes newlines and control characters).
     * Use for username, email, title – fields that must not span multiple lines.
     *
     * @param input the raw user-supplied string
     * @return sanitized single-line string or null
     */
    public static String sanitizeLine(String input) {
        if (input == null) return null;
        return input.replaceAll("[\\x00-\\x1F\\x7F]", "").strip();
    }

    /**
     * Sanitizes a multi-line text field (note content, comments).
     * Preserves newlines and tabs but removes other control characters.
     *
     * @param input the raw user-supplied multi-line string
     * @return sanitized string or null
     */
    public static String sanitizeMultiline(String input) {
        return sanitize(input);
    }

    /**
     * Sanitizes a filename for storage in the database.
     * Removes path separators, null bytes, and other dangerous characters.
     *
     * @param filename the original filename from the upload
     * @return safe display name (still output-encoded in JSP)
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null) return "unnamed";
        // Strip path traversal attempts and control characters
        String name = filename.replaceAll("[/\\\\:*?\"<>|\\x00-\\x1F\\x7F]", "_");
        // Limit length
        return name.length() > 255 ? name.substring(0, 255) : name;
    }
}
