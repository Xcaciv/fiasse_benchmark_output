package com.loosenotes.util;

import java.util.regex.Pattern;

/**
 * SSEM: Input Validation — centralised server-side validation helpers.
 * All user-supplied input is validated before being processed or stored.
 */
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_\\-]{3,50}$");

    private ValidationUtil() {}

    /** Returns true if the string is null or blank. */
    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Validate an email address format. */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /** Validate a username: 3–50 alphanumeric/underscore/hyphen characters. */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * Truncate a string to maxLen characters if it exceeds that length.
     * Never returns null.
     */
    public static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    /**
     * Parse a long from a request parameter safely.
     * Returns -1 on failure.
     */
    public static long parseLong(String s) {
        if (s == null) return -1;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Parse an int from a request parameter safely.
     * Returns -1 on failure.
     */
    public static int parseInt(String s) {
        if (s == null) return -1;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
