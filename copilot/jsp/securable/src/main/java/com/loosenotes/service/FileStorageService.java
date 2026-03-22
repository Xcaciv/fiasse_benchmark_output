package com.loosenotes.service;

import com.loosenotes.util.AppUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.Part;

public final class FileStorageService {
    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt", "png", "jpg", "jpeg");
    private static Path storageDirectory;

    private FileStorageService() {
    }

    public static synchronized void initialize(Path directory) throws IOException {
        if (storageDirectory != null) {
            return;
        }
        Files.createDirectories(directory);
        storageDirectory = directory.toAbsolutePath().normalize();
    }

    public static StoredFile store(Part part) throws IOException {
        if (storageDirectory == null) {
            throw new IllegalStateException("Storage directory has not been initialized.");
        }
        String originalName = part.getSubmittedFileName();
        if (originalName == null || originalName.isBlank() || part.getSize() == 0) {
            return null;
        }
        originalName = Path.of(originalName).getFileName().toString();
        String extension = extension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IOException("Unsupported attachment type: " + extension);
        }
        if (part.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IOException("Attachment exceeds the 5 MB upload limit.");
        }
        String storedName = UUID.randomUUID() + "." + extension;
        Path destination = storageDirectory.resolve(storedName).normalize();
        if (!destination.startsWith(storageDirectory)) {
            throw new IOException("Invalid storage path.");
        }
        try (InputStream inputStream = part.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        String contentType = AppUtil.trimToEmpty(part.getContentType());
        if (contentType.isBlank()) {
            contentType = "application/octet-stream";
        }
        return new StoredFile(storedName, originalName, contentType, part.getSize());
    }

    public static Path resolve(String storedName) {
        Path path = storageDirectory.resolve(storedName).normalize();
        if (!path.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid stored file path.");
        }
        return path;
    }

    public static void deleteIfExists(String storedName) throws IOException {
        Files.deleteIfExists(resolve(storedName));
    }

    private static String extension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    public static final class StoredFile {
        private final String storedName;
        private final String originalName;
        private final String contentType;
        private final long sizeBytes;

        public StoredFile(String storedName, String originalName, String contentType, long sizeBytes) {
            this.storedName = storedName;
            this.originalName = originalName;
            this.contentType = contentType;
            this.sizeBytes = sizeBytes;
        }

        public String getStoredName() {
            return storedName;
        }

        public String getOriginalName() {
            return originalName;
        }

        public String getContentType() {
            return contentType;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }
    }
}
