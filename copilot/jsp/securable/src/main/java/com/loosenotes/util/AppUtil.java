package com.loosenotes.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class AppUtil {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private AppUtil() {
    }

    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public static String likeValue(String value) {
        return "%" + trimToEmpty(value).toLowerCase(Locale.ROOT) + "%";
    }

    public static String excerpt(String text, int maxLength) {
        String normalized = trimToEmpty(text).replace("", "");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    public static String encodeFileName(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public static List<String> validateRegistration(String username, String email, String password, String confirmPassword) {
        List<String> errors = new ArrayList<>();
        validateUsername(username, errors);
        validateEmail(email, errors);
        validatePassword(password, errors);
        if (!trimToEmpty(password).equals(confirmPassword == null ? "" : confirmPassword)) {
            errors.add("Password confirmation does not match.");
        }
        return errors;
    }

    public static void validateUsername(String username, List<String> errors) {
        String value = trimToEmpty(username);
        if (value.length() < 3 || value.length() > 30) {
            errors.add("Username must be between 3 and 30 characters.");
        }
        if (!value.matches("[A-Za-z0-9._-]+")) {
            errors.add("Username may contain only letters, numbers, dots, underscores, and hyphens.");
        }
    }

    public static void validateEmail(String email, List<String> errors) {
        String value = trimToEmpty(email);
        if (value.isBlank() || value.length() > 255 || !EMAIL_PATTERN.matcher(value).matches()) {
            errors.add("Enter a valid email address.");
        }
    }

    public static void validatePassword(String password, List<String> errors) {
        String value = password == null ? "" : password;
        if (value.length() < 8) {
            errors.add("Password must be at least 8 characters.");
        }
        if (!value.matches(".*[a-z].*")) {
            errors.add("Password must include a lowercase letter.");
        }
        if (!value.matches(".*[A-Z].*")) {
            errors.add("Password must include an uppercase letter.");
        }
        if (!value.matches(".*\d.*")) {
            errors.add("Password must include a digit.");
        }
        if (!value.matches(".*[^A-Za-z0-9].*")) {
            errors.add("Password must include a special character.");
        }
    }

    public static List<String> validateNote(String title, String content) {
        List<String> errors = new ArrayList<>();
        String cleanTitle = trimToEmpty(title);
        String cleanContent = trimToEmpty(content);
        if (cleanTitle.isBlank() || cleanTitle.length() > 150) {
            errors.add("Title is required and must be 150 characters or fewer.");
        }
        if (cleanContent.isBlank() || cleanContent.length() > 20_000) {
            errors.add("Content is required and must be 20,000 characters or fewer.");
        }
        return errors;
    }
}
