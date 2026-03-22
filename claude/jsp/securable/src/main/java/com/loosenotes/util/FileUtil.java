package com.loosenotes.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SSEM: Secure Storage — file uploads stored with UUID names (no path traversal),
 * strict extension allow-list, and size enforcement.
 */
public class FileUtil {

    private static final Logger LOGGER = Logger.getLogger(FileUtil.class.getName());

    /** Absolute maximum upload size: 10 MB. */
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    /** Allowed file extensions (lower-case). */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "doc", "docx", "txt", "png", "jpg", "jpeg"
    ));

    private static final String UPLOAD_DIR =
            System.getProperty("user.home") + "/.loosenotes/uploads";

    private FileUtil() {}

    /** Initialise upload directory on first use. */
    public static void ensureUploadDir() {
        new File(UPLOAD_DIR).mkdirs();
    }

    /**
     * Validate the original filename.
     * Returns null on success, or an error message on failure.
     */
    public static String validateFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "Filename must not be empty.";
        }
        String ext = getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            return "File type not allowed. Allowed: pdf, doc, docx, txt, png, jpg, jpeg.";
        }
        return null;
    }

    /**
     * Store a file from an InputStream under a UUID-based name.
     * Returns the stored filename (UUID + extension).
     */
    public static String store(InputStream inputStream, String originalFilename)
            throws IOException {
        ensureUploadDir();
        String ext = getExtension(originalFilename);
        String storedName = UUID.randomUUID().toString() + "." + ext;
        Path dest = Paths.get(UPLOAD_DIR, storedName);
        Files.copy(inputStream, dest, StandardCopyOption.REPLACE_EXISTING);
        return storedName;
    }

    /**
     * Resolve a stored filename to its absolute path, preventing path traversal.
     * Returns null if the filename is unsafe.
     */
    public static File resolve(String storedFilename) {
        if (storedFilename == null || storedFilename.contains("..") || storedFilename.contains("/")
                || storedFilename.contains("\\")) {
            return null;
        }
        File f = new File(UPLOAD_DIR, storedFilename);
        // Canonical path check against upload dir
        try {
            String canonical = f.getCanonicalPath();
            String uploadDirCanonical = new File(UPLOAD_DIR).getCanonicalPath();
            if (!canonical.startsWith(uploadDirCanonical + File.separator)
                    && !canonical.equals(uploadDirCanonical)) {
                LOGGER.warning("Path traversal attempt blocked: " + storedFilename);
                return null;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Cannot resolve canonical path for: " + storedFilename, e);
            return null;
        }
        return f;
    }

    /** Delete a stored file silently. */
    public static void delete(String storedFilename) {
        File f = resolve(storedFilename);
        if (f != null && f.exists()) {
            f.delete();
        }
    }

    /** Extract lower-case extension from filename; returns "" if none. */
    public static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }
}
