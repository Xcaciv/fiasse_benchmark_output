package com.loosenotes.util;

import javax.servlet.ServletContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * File validation and path-safety utilities.
 * Validates extension, size, and prevents path-traversal attacks.
 */
public final class FileUtil {

    public static final Set<String> ALLOWED_EXTENSIONS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList("pdf", "doc", "docx", "txt", "png", "jpg", "jpeg"))
    );

    /** Maximum file size: 10 MB. */
    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private FileUtil() {}

    /** Returns true if the file extension is in the allowed set. */
    public static boolean isAllowedExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        return ALLOWED_EXTENSIONS.contains(getExtension(filename));
    }

    /** Returns true if the file size is within the configured limit. */
    public static boolean validateFileSize(long size) {
        return size > 0 && size <= MAX_FILE_SIZE;
    }

    /** Extracts and lowercases the file extension. Returns empty string if none found. */
    public static String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * Returns the upload directory from the ServletContext init param "uploadDir",
     * defaulting to ${user.home}/.loosenotes/uploads.
     */
    public static String getUploadDir(ServletContext ctx) {
        String configured = ctx.getInitParameter("uploadDir");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return System.getProperty("user.home") + "/.loosenotes/uploads";
    }

    /**
     * Strips path traversal characters and allows only safe filename characters.
     * Derived Integrity Principle: server controls stored path; client name is display-only.
     */
    public static String sanitizeFilename(String name) {
        if (name == null) {
            return "file";
        }
        // Strip directory separators and null bytes first
        String stripped = name.replaceAll("[/\\\\:*?\"<>|\\x00]", "_");
        // Allow only alphanumeric, dash, underscore, dot
        String safe = stripped.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        // Prevent leading dots (hidden files)
        if (safe.startsWith(".")) {
            safe = "_" + safe;
        }
        return safe.isEmpty() ? "file" : safe;
    }
}
