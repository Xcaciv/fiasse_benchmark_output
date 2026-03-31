package com.loosenotes.util;

import java.util.regex.Pattern;

/**
 * Centralized input validation rules.
 *
 * SSEM / ASVS alignment:
 * - ASVS V2.1 (Validation Documentation): rules documented here.
 * - Integrity: canonicalize → validate pattern.
 * - Analyzability: each method is a single predicate.
 * - Testability: pure static methods, no side effects.
 *
 * Trust boundary: these methods are called at servlet entry points BEFORE
 * any business logic or database access.
 */
public final class ValidationUtil {

    // Email: simplified RFC 5321 pattern; rejects obviously invalid addresses
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    // Username: alphanumeric + underscore + hyphen, 3–50 chars
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_\\-]{3,50}$");

    private ValidationUtil() {}

    /**
     * Returns true if the email address matches the application's accepted format.
     * Caller must HTML-encode the value before rendering.
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        String trimmed = email.strip();
        return trimmed.length() <= 254 && EMAIL_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Returns true if username matches the accepted pattern (3–50 chars,
     * alphanumeric, underscore, hyphen only).
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.isBlank()) return false;
        return USERNAME_PATTERN.matcher(username.strip()).matches();
    }

    /**
     * Returns true if the note title is non-blank and within the maximum length.
     *
     * @param title     the title to validate
     * @param maxLength maximum allowed length in characters
     */
    public static boolean isValidNoteTitle(String title, int maxLength) {
        if (title == null || title.isBlank()) return false;
        return title.strip().length() <= maxLength;
    }

    /**
     * Returns true if the content is non-blank and within the byte limit.
     *
     * @param content       the note body
     * @param maxLengthBytes maximum allowed UTF-8 byte length
     */
    public static boolean isValidNoteContent(String content, int maxLengthBytes) {
        if (content == null || content.isBlank()) return false;
        return content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= maxLengthBytes;
    }

    /**
     * Returns true if the rating value is within the allowed 1–5 range.
     */
    public static boolean isValidRating(int stars) {
        return stars >= 1 && stars <= 5;
    }

    /**
     * Returns a trimmed, non-null string or null if blank.
     * Canonical form step before further validation.
     */
    public static String trimOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Parses a long from a string path segment safely.
     * Returns -1 on parse failure (caller treats as bad request).
     */
    public static long parseLongId(String value) {
        if (value == null || value.isBlank()) return -1L;
        try {
            long id = Long.parseLong(value.strip());
            return id > 0 ? id : -1L;
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
