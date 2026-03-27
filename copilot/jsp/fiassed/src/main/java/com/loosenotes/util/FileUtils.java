package com.loosenotes.util;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

public final class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "image/png",
            "image/jpeg"
    );
    private static final Tika TIKA = new Tika();

    private FileUtils() {}

    public static String sanitizeFilename(String filename) {
        if (filename == null) return "file";
        // Strip path separators, null bytes, and dangerous chars
        String sanitized = filename
                .replaceAll("[\\x00/\\\\:*?\"<>|]", "_")
                .replaceAll("\\.\\.+", ".")
                .trim();
        if (sanitized.isEmpty()) return "file";
        // Limit length
        if (sanitized.length() > 100) sanitized = sanitized.substring(sanitized.length() - 100);
        return sanitized;
    }

    public static boolean isAllowedMimeType(String mimeType) {
        return mimeType != null && ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase());
    }

    public static String detectMimeType(InputStream inputStream) throws IOException {
        return TIKA.detect(inputStream);
    }

    public static Path getUploadDirectory() {
        String base = System.getProperty("catalina.home");
        if (base == null) base = System.getProperty("java.io.tmpdir");
        Path dir = Paths.get(base, "uploads", "loosenotes");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            logger.error("Failed to create upload directory: {}", dir, e);
        }
        return dir;
    }

    public static String generateStoredFilename(String originalFilename) {
        String ext = "";
        int dot = originalFilename.lastIndexOf('.');
        if (dot >= 0) ext = originalFilename.substring(dot).toLowerCase();
        return UUID.randomUUID().toString() + ext;
    }

    public static long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }
}
