package com.loosenotes.util;

import java.util.regex.Pattern;

/**
 * Input validation utilities (Stage 3 of the trust boundary pipeline).
 * SSEM: Integrity - format and constraint enforcement after sanitization.
 * SSEM: Analyzability - each method validates exactly one concern.
 */
public final class ValidationUtil {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]{3,50}$");
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern SAFE_TOKEN_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]{32,128}$");

    private static final int NOTE_TITLE_MAX  = 255;
    private static final int NOTE_CONTENT_MAX = 50_000;
    private static final int COMMENT_MAX     = 1_000;
    private static final int USERNAME_MAX    = 50;
    private static final int EMAIL_MAX       = 255;

    private ValidationUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Validates a username: 3-50 alphanumeric/underscore/hyphen characters.
     */
    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        return USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * Validates an email address format.
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.length() > EMAIL_MAX) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validates note title: non-empty, within length limit.
     */
    public static boolean isValidNoteTitle(String title) {
        if (title == null || title.isBlank()) return false;
        return title.length() <= NOTE_TITLE_MAX;
    }

    /**
     * Validates note content: non-empty, within length limit.
     */
    public static boolean isValidNoteContent(String content) {
        if (content == null || content.isBlank()) return false;
        return content.length() <= NOTE_CONTENT_MAX;
    }

    /**
     * Validates a rating value: must be 1-5 inclusive.
     */
    public static boolean isValidRatingValue(int value) {
        return value >= 1 && value <= 5;
    }

    /**
     * Validates a rating comment: optional but bounded in length.
     */
    public static boolean isValidRatingComment(String comment) {
        if (comment == null || comment.isBlank()) return true; // Optional
        return comment.length() <= COMMENT_MAX;
    }

    /**
     * Validates that a share/reset token has the expected safe format.
     * Prevents path traversal or injection via token parameters.
     */
    public static boolean isValidToken(String token) {
        if (token == null) return false;
        return SAFE_TOKEN_PATTERN.matcher(token).matches();
    }

    /**
     * Validates a database ID: must be positive.
     */
    public static boolean isValidId(long id) {
        return id > 0;
    }

    /**
     * Parses a long from a string, returning -1 if invalid.
     * Prevents NumberFormatException from propagating to callers.
     */
    public static long parseLongSafe(String value) {
        if (value == null || value.isBlank()) return -1L;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    /**
     * Parses an int from a string, returning defaultValue if invalid.
     */
    public static int parseIntSafe(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
