package com.loosenotes.service;

import com.loosenotes.dao.AttachmentDao;
import com.loosenotes.dao.NoteDao;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Handles file attachment upload and retrieval.
 * SSEM: Integrity - validates extension, size, and content type.
 * SSEM: Confidentiality - stored filename is UUID (not user-controlled).
 * SSEM: Availability - enforces max file size.
 * SSEM: Resilience - path traversal prevention via stored UUID filenames.
 */
public class AttachmentService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);

    /** Allowed file extensions. Content-type is also checked independently. */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "pdf", "doc", "docx", "txt", "png", "jpg", "jpeg"
    );

    /** Allowed MIME types corresponding to allowed extensions. */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain",
        "image/png",
        "image/jpeg"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB

    private final AttachmentDao attachmentDao;
    private final NoteDao noteDao;
    private final Path uploadDirectory;

    public AttachmentService(AttachmentDao attachmentDao, NoteDao noteDao, String uploadDir) {
        this.attachmentDao = attachmentDao;
        this.noteDao = noteDao;
        this.uploadDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        ensureUploadDirectory();
    }

    /**
     * Stores an uploaded file and records metadata in the database.
     * Trust boundary: validates extension and size before writing to disk.
     *
     * @param noteId       target note
     * @param ownerId      user performing the upload
     * @param rawFilename  original filename from the upload (sanitized internally)
     * @param contentType  declared MIME type from the multipart part
     * @param inputStream  file bytes
     * @param fileSize     declared file size
     * @return the created Attachment
     */
    public Attachment upload(long noteId, long ownerId, String rawFilename,
                              String contentType, InputStream inputStream,
                              long fileSize) throws IOException, SQLException {
        verifyNoteOwnership(noteId, ownerId);
        String safeFilename = InputSanitizer.sanitizeFilename(rawFilename);
        validateUpload(safeFilename, contentType, fileSize);

        String storedName = UUID.randomUUID().toString() + extractExtension(safeFilename);
        Path targetPath = resolveStoredPath(storedName);

        writeFile(inputStream, targetPath, fileSize);

        Attachment attachment = new Attachment();
        attachment.setNoteId(noteId);
        attachment.setOriginalFilename(safeFilename);
        attachment.setStoredFilename(storedName);
        attachment.setFileSize(fileSize);
        attachment.setContentType(normalizeContentType(contentType));
        long id = attachmentDao.create(attachment);
        attachment.setId(id);
        return attachment;
    }

    /**
     * Returns the file path for download.
     * Validates that the stored path is within the upload directory (path traversal prevention).
     *
     * @param attachmentId  attachment to download
     * @param requestingUserId  user making the request (0 for share link access)
     * @param isShareAccess whether the request comes via a share link
     */
    public Optional<Path> getFilePath(long attachmentId, long requestingUserId,
                                       boolean isShareAccess) throws SQLException {
        Optional<Attachment> opt = attachmentDao.findById(attachmentId);
        if (opt.isEmpty()) return Optional.empty();

        Attachment attachment = opt.get();
        if (!isShareAccess) {
            verifyNoteReadAccess(attachment.getNoteId(), requestingUserId);
        }

        Path filePath = resolveStoredPath(attachment.getStoredFilename());
        if (!Files.exists(filePath)) {
            log.warn("Attachment file missing on disk: id={}", attachmentId);
            return Optional.empty();
        }
        return Optional.of(filePath);
    }

    /** Returns attachment metadata by ID. */
    public Optional<Attachment> findById(long id) throws SQLException {
        return attachmentDao.findById(id);
    }

    private void verifyNoteOwnership(long noteId, long userId) throws SQLException {
        Optional<Note> note = noteDao.findById(noteId);
        if (note.isEmpty() || note.get().getUserId() != userId) {
            throw new SecurityException("Note not found or access denied");
        }
    }

    private void verifyNoteReadAccess(long noteId, long userId) throws SQLException {
        Optional<Note> note = noteDao.findById(noteId);
        if (note.isEmpty()) throw new SecurityException("Note not found");
        if (note.get().getUserId() != userId && !note.get().isPublic()) {
            throw new SecurityException("Access denied to note attachment");
        }
    }

    private void validateUpload(String filename, String contentType, long fileSize) {
        if (fileSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds maximum size of 10 MB");
        }
        String ext = extractExtension(filename).toLowerCase().replace(".", "");
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("File type not allowed: " + ext);
        }
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(normalizeContentType(contentType))) {
            throw new IllegalArgumentException("Content type not allowed: " + contentType);
        }
    }

    private void writeFile(InputStream in, Path target, long expectedSize) throws IOException {
        long written = Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        if (written != expectedSize) {
            Files.deleteIfExists(target);
            throw new IOException("File size mismatch: expected " + expectedSize + " got " + written);
        }
    }

    private Path resolveStoredPath(String storedFilename) {
        // Resolve and normalize - ensure it stays within upload directory
        Path resolved = uploadDirectory.resolve(storedFilename).normalize();
        if (!resolved.startsWith(uploadDirectory)) {
            throw new SecurityException("Path traversal attempt detected");
        }
        return resolved;
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot) : "";
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) return "application/octet-stream";
        int semi = contentType.indexOf(';');
        return semi >= 0 ? contentType.substring(0, semi).trim() : contentType.trim();
    }

    private void ensureUploadDirectory() {
        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload directory: " + uploadDirectory, e);
        }
    }
}
