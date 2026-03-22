package com.loosenotes.service.impl;

import com.loosenotes.service.FileService;
import com.loosenotes.service.ServiceException;
import com.loosenotes.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;

/**
 * Stores uploaded files with UUID-based names to prevent collisions and path traversal.
 * Validates size limits before writing (Availability, Integrity).
 */
public final class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    private final String uploadDir;
    private final long maxSizeBytes;

    public FileServiceImpl(String uploadDir, long maxSizeBytes) {
        this.uploadDir    = uploadDir;
        this.maxSizeBytes = maxSizeBytes;
        ensureDirectoryExists();
    }

    @Override
    public String store(String originalFilename, InputStream inputStream, long fileSize) {
        ValidationUtil.validateFileExtension(originalFilename);
        if (fileSize > maxSizeBytes) {
            throw new ServiceException(ServiceException.ErrorCode.VALIDATION_ERROR,
                    "File exceeds maximum allowed size of " + maxSizeBytes + " bytes");
        }

        String extension = getExtension(originalFilename);
        // UUID name prevents path traversal and collisions (Integrity)
        String storedName = UUID.randomUUID().toString() + "." + extension;
        Path target = Paths.get(uploadDir, storedName);

        try {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored: {}", storedName);
            return storedName;
        } catch (IOException e) {
            log.error("Failed to store file", e);
            throw new ServiceException(ServiceException.ErrorCode.STORAGE_ERROR,
                    "Could not store file");
        }
    }

    @Override
    public void retrieve(String storedFilename, OutputStream outputStream) {
        // Validate stored name contains no path separators (Integrity: prevent traversal)
        if (storedFilename.contains("/") || storedFilename.contains("\\")
                || storedFilename.contains("..")) {
            throw new ServiceException(ServiceException.ErrorCode.VALIDATION_ERROR,
                    "Invalid stored filename");
        }
        Path source = Paths.get(uploadDir, storedFilename);
        if (!source.normalize().startsWith(Paths.get(uploadDir).normalize())) {
            throw new ServiceException(ServiceException.ErrorCode.VALIDATION_ERROR,
                    "Path traversal detected");
        }
        try {
            Files.copy(source, outputStream);
        } catch (IOException e) {
            log.error("Failed to retrieve file: {}", storedFilename, e);
            throw new ServiceException(ServiceException.ErrorCode.NOT_FOUND,
                    "File not found");
        }
    }

    @Override
    public boolean delete(String storedFilename) {
        if (storedFilename == null || storedFilename.contains("..")) return false;
        Path target = Paths.get(uploadDir, storedFilename);
        try {
            return Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", storedFilename, e);
            return false;
        }
    }

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            log.error("Cannot create upload directory: {}", uploadDir, e);
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "bin";
    }
}
