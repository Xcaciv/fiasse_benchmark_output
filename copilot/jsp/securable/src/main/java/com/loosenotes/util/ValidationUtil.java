package com.loosenotes.util;

import java.util.regex.Pattern;

/**
 * Input validation and sanitisation utilities.
 * All validation methods are pure functions — no state.
 * Trust boundary: call sanitizeString before any other validation.
 */
public final class ValidationUtil {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 50000;
    private static final int MAX_COMMENT_LENGTH = 1000;

    private ValidationUtil() {}

    /** Username: 3–30 chars, alphanumeric plus underscore only. */
    public static boolean isValidUsername(String s) {
        return s != null && USERNAME_PATTERN.matcher(s).matches();
    }

    /** Basic RFC-5322 inspired email check. */
    public static boolean isValidEmail(String s) {
        return s != null && EMAIL_PATTERN.matcher(s).matches();
    }

    /** Title: 1–200 chars, not blank. */
    public static boolean isValidTitle(String s) {
        return s != null && !s.isBlank() && s.length() <= MAX_TITLE_LENGTH;
    }

    /** Content: not null, max 50 000 chars. */
    public static boolean isValidContent(String s) {
        return s != null && s.length() <= MAX_CONTENT_LENGTH;
    }

    /** Rating value: 1–5 inclusive. */
    public static boolean isValidRating(int r) {
        return r >= 1 && r <= 5;
    }

    /** Comment: optional, max 1 000 chars. */
    public static boolean isValidComment(String s) {
        return s == null || s.length() <= MAX_COMMENT_LENGTH;
    }

    /**
     * Removes null bytes and ASCII control characters (0x00–0x1F except tab/newline/CR).
     * Trim is applied first; result is safe for further validation.
     */
    public static String sanitizeString(String s) {
        if (s == null) {
            return null;
        }
        return s.trim().replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
    }

    /** Returns true if the string is null or contains only whitespace. */
    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
