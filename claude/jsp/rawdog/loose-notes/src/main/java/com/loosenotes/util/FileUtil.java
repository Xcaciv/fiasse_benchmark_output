package com.loosenotes.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileUtil {

    private static final Logger logger = Logger.getLogger(FileUtil.class.getName());
    private static final String UPLOADS_DIR = System.getProperty("user.home") + "/.loosenotes/uploads/";

    static {
        File dir = new File(UPLOADS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Get the uploads directory path.
     *
     * @return absolute path to uploads directory
     */
    public static String getUploadsDir() {
        return UPLOADS_DIR;
    }

    /**
     * Generate a unique stored filename for an uploaded file.
     *
     * @param originalFilename the original file name
     * @return a unique stored filename preserving the extension
     */
    public static String generateStoredFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Save an uploaded file to the uploads directory.
     *
     * @param inputStream    the input stream of the uploaded file
     * @param storedFilename the name to store the file as
     * @return true if saved successfully
     */
    public static boolean saveFile(InputStream inputStream, String storedFilename) {
        try {
            Path targetPath = Paths.get(UPLOADS_DIR, storedFilename);
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save file: " + storedFilename, e);
            return false;
        }
    }

    /**
     * Get the file object for a stored file.
     *
     * @param storedFilename the stored filename
     * @return File object
     */
    public static File getFile(String storedFilename) {
        return new File(UPLOADS_DIR + storedFilename);
    }

    /**
     * Delete a stored file from disk.
     *
     * @param storedFilename the stored filename to delete
     * @return true if deleted successfully
     */
    public static boolean deleteFile(String storedFilename) {
        if (storedFilename == null || storedFilename.isEmpty()) {
            return false;
        }
        try {
            Path filePath = Paths.get(UPLOADS_DIR, storedFilename);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to delete file: " + storedFilename, e);
            return false;
        }
    }

    /**
     * Stream a file to an output stream.
     *
     * @param storedFilename the stored filename
     * @param outputStream   the output stream to write to
     * @throws IOException if reading or writing fails
     */
    public static void streamFile(String storedFilename, OutputStream outputStream) throws IOException {
        Path filePath = Paths.get(UPLOADS_DIR, storedFilename);
        Files.copy(filePath, outputStream);
    }

    /**
     * Check if a file exists in uploads.
     *
     * @param storedFilename the stored filename
     * @return true if file exists
     */
    public static boolean fileExists(String storedFilename) {
        return new File(UPLOADS_DIR + storedFilename).exists();
    }

    /**
     * Get a safe filename, stripping path components.
     *
     * @param filename the input filename
     * @return sanitized filename
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }
        // Strip path separators to prevent directory traversal
        return Paths.get(filename).getFileName().toString();
    }

    /**
     * Format file size for display.
     *
     * @param bytes the file size in bytes
     * @return human readable file size
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
    }
}
