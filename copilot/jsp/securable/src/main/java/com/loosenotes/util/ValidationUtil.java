package com.loosenotes.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * Input validation utilities applied at every trust boundary (Integrity).
 * Implements canonicalize → sanitize → validate pipeline.
 */
public final class ValidationUtil {

    private static final int MAX_USERNAME_LEN = 50;
    private static final int MAX_EMAIL_LEN    = 255;
    private static final int MAX_TITLE_LEN    = 200;
    private static final int MAX_CONTENT_LEN  = 100_000;
    private static final int MAX_COMMENT_LEN  = 1000;
    private static final int MIN_PASSWORD_LEN = 8;

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_\\-]{3,50}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final Set<String> ALLOWED_EXTENSIONS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList("pdf","doc","docx","txt","png","jpg","jpeg"))
    );

    private ValidationUtil() {}

    /** Returns trimmed value or throws if blank/too long. */
    public static String requireNonBlank(String value, String fieldName, int maxLen) {
        if (value == null) {
            throw new ValidationException(fieldName + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException(fieldName + " is required");
        }
        if (trimmed.length() > maxLen) {
            throw new ValidationException(fieldName + " exceeds maximum length of " + maxLen);
        }
        return trimmed;
    }

    public static String validateUsername(String username) {
        String value = requireNonBlank(username, "Username", MAX_USERNAME_LEN);
        if (!USERNAME_PATTERN.matcher(value).matches()) {
            throw new ValidationException(
                "Username must be 3–50 characters: letters, digits, underscore, hyphen");
        }
        return value;
    }

    public static String validateEmail(String email) {
        String value = requireNonBlank(email, "Email", MAX_EMAIL_LEN);
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new ValidationException("Invalid email address");
        }
        return value.toLowerCase();
    }

    public static String validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LEN) {
            throw new ValidationException(
                "Password must be at least " + MIN_PASSWORD_LEN + " characters");
        }
        return password; // Do not trim passwords
    }

    public static String validateTitle(String title) {
        return requireNonBlank(title, "Title", MAX_TITLE_LEN);
    }

    public static String validateContent(String content) {
        return requireNonBlank(content, "Content", MAX_CONTENT_LEN);
    }

    public static String validateComment(String comment) {
        if (comment == null) return null;
        String trimmed = comment.trim();
        if (trimmed.length() > MAX_COMMENT_LEN) {
            throw new ValidationException("Comment exceeds maximum length of " + MAX_COMMENT_LEN);
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static int validateStars(String starsParam) {
        try {
            int stars = Integer.parseInt(starsParam);
            if (stars < 1 || stars > 5) throw new ValidationException("Rating must be 1–5 stars");
            return stars;
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid rating value");
        }
    }

    /**
     * Validates file extension is in the allowed set (Integrity: file validation).
     * Extension is lowercased and checked against allowlist.
     */
    public static void validateFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new ValidationException("File must have a recognized extension");
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new ValidationException(
                "File type not allowed. Permitted: " + ALLOWED_EXTENSIONS);
        }
    }

    /** Parses a positive long ID from a path segment. */
    public static long parseId(String segment, String fieldName) {
        try {
            long id = Long.parseLong(segment.trim());
            if (id <= 0) throw new ValidationException("Invalid " + fieldName);
            return id;
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid " + fieldName);
        }
    }

    /** Lightweight checked exception for validation failures. */
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}
