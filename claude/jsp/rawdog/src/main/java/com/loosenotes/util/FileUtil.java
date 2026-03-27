package com.loosenotes.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FileUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
        "pdf", "doc", "docx", "txt", "png", "jpg", "jpeg"
    ));

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private FileUtil() {}

    public static boolean isAllowedExtension(String filename) {
        if (filename == null || filename.isEmpty()) return false;
        int dotIdx = filename.lastIndexOf('.');
        if (dotIdx < 0) return false;
        String ext = filename.substring(dotIdx + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    public static boolean isWithinSizeLimit(long size) {
        return size > 0 && size <= MAX_FILE_SIZE;
    }

    public static String getExtension(String filename) {
        if (filename == null) return "";
        int dotIdx = filename.lastIndexOf('.');
        return dotIdx >= 0 ? filename.substring(dotIdx + 1).toLowerCase() : "";
    }

    public static String generateStoredFilename(String originalFilename) {
        String ext = getExtension(originalFilename);
        return UUID.randomUUID().toString() + (ext.isEmpty() ? "" : "." + ext);
    }

    public static String saveFile(InputStream inputStream, String storedFilename) throws IOException {
        Path uploadPath = Paths.get(DBUtil.UPLOAD_DIR);
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(storedFilename);
        Files.copy(inputStream, filePath);
        logger.info("File saved: {}", filePath);
        return filePath.toString();
    }

    public static boolean deleteFile(String storedFilename) {
        try {
            Path filePath = Paths.get(DBUtil.UPLOAD_DIR, storedFilename);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            logger.error("Failed to delete file: {}", storedFilename, e);
            return false;
        }
    }

    public static File getFile(String storedFilename) {
        return Paths.get(DBUtil.UPLOAD_DIR, storedFilename).toFile();
    }

    public static String getContentType(String filename) {
        String ext = getExtension(filename);
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt" -> "text/plain";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }
}
