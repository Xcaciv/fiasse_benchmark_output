package com.loosenotes.util;

import java.util.regex.Pattern;

public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[A-Za-z0-9_]{3,50}$"
    );

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static boolean isValidRating(int rating) {
        return rating >= 1 && rating <= 5;
    }

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("<", "&lt;")
                   .replaceAll(">", "&gt;")
                   .replaceAll("\"", "&quot;")
                   .replaceAll("'", "&#x27;")
                   .replaceAll("/", "&#x2F;");
    }

    public static String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("&", "&amp;")
                   .replaceAll("<", "&lt;")
                   .replaceAll(">", "&gt;")
                   .replaceAll("\"", "&quot;")
                   .replaceAll("'", "&#39;");
    }

    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}
