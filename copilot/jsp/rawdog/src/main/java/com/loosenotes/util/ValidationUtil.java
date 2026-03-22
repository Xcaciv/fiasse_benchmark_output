package com.loosenotes.util;

import java.util.Locale;
import java.util.Set;

public final class ValidationUtil {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt", "png", "jpg", "jpeg");

    private ValidationUtil() {
    }

    public static String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static void requireUsername(String username) {
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long.");
        }
    }

    public static void requireEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
    }

    public static void requirePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }
    }

    public static void requireTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required.");
        }
    }

    public static void requireContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content is required.");
        }
    }

    public static void requireRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
    }

    public static boolean isAllowedExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    public static String allowedExtensionsLabel() {
        return String.join(", ", ALLOWED_EXTENSIONS);
    }
}
