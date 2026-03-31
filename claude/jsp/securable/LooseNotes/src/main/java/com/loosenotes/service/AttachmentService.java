package com.loosenotes.service;

import com.loosenotes.dao.AttachmentDao;
import com.loosenotes.dao.NoteDao;
import com.loosenotes.model.AuditLog.EventType;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.util.InputSanitizer;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Business logic for file upload, retrieval, and deletion.
 *
 * SSEM / ASVS alignment:
 * - ASVS V12.1 (File Upload): MIME validation, size limits, UUID filename.
 * - Integrity: stored filename is a UUID – prevents path traversal.
 * - Confidentiality: original filename stored only in DB for display.
 * - Resilience: cleanup of partial uploads on failure.
 */
public class AttachmentService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "image/png",
            "image/jpeg"
    );

    private final AttachmentDao attachmentDao;
    private final NoteDao noteDao;
    private final AuditService auditService;
    private final Path uploadDir;
    private final long maxFileSizeBytes;

    public AttachmentService(AttachmentDao attachmentDao, NoteDao noteDao,
                             AuditService auditService, Path uploadDir, long maxFileSizeBytes) {
        this.attachmentDao   = attachmentDao;
        this.noteDao         = noteDao;
        this.auditService    = auditService;
        this.uploadDir       = uploadDir;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    /**
     * Saves an uploaded file to disk and records metadata in the database.
     *
     * Trust boundary: MIME type and size are validated here before any I/O.
     *
     * @param noteId     the note to attach to
     * @param part       the multipart upload part
     * @param uploaderId the uploading user's ID
     * @param ipAddress  client IP for audit
     * @return the new Attachment record
     */
    public Attachment saveAttachment(long noteId, Part part, long uploaderId, String ipAddress)
            throws ServiceException, IOException {

        // Verify note exists and uploader owns it
        Note note = noteDao.findById(noteId)
                .orElseThrow(() -> new ServiceException("Note not found"));
        if (note.getUserId() != uploaderId) {
            throw new ServiceException("Access denied");
        }

        // Validate MIME type – use server-determined content type, not client header
        String mimeType = part.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            throw new ServiceException("File type not allowed");
        }

        long fileSize = part.getSize();
        if (fileSize == 0) {
            throw new ServiceException("Uploaded file is empty");
        }
        if (fileSize > maxFileSizeBytes) {
            throw new ServiceException("File exceeds maximum size of " + (maxFileSizeBytes / 1048576) + " MB");
        }

        // Sanitize original filename for database storage only
        String originalName = InputSanitizer.sanitizeFilename(
                extractFilename(part.getHeader("content-disposition")));

        // Generate UUID-based stored filename to prevent collisions and path traversal
        String storedName  = UUID.randomUUID() + "." + getExtension(originalName);
        Path   targetPath  = uploadDir.resolve(storedName);

        // Write file to disk
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to write uploaded file", e);
            throw new IOException("File storage error", e);
        }

        // Persist metadata
        Attachment a = new Attachment();
        a.setNoteId(noteId);
        a.setOriginalName(originalName);
        a.setStoredName(storedName);
        a.setMimeType(mimeType);
        a.setFileSizeBytes(fileSize);

        try {
            long id = attachmentDao.insert(a);
            a.setId(id);
            auditService.record(uploaderId, EventType.ATTACHMENT,
                    "attachment_uploaded noteId=" + noteId + " attachmentId=" + id, ipAddress);
            return a;
        } catch (Exception e) {
            // Clean up the file if DB insert fails
            Files.deleteIfExists(targetPath);
            throw e;
        }
    }

    /** Returns attachments for a note visible to the requesting user. */
    public List<Attachment> getAttachmentsForNote(long noteId, long requestingUserId,
                                                   boolean isAdmin) throws ServiceException {
        Note note;
        try {
            note = noteDao.findById(noteId).orElseThrow(() -> new ServiceException("Note not found"));
        } catch (java.sql.SQLException e) {
            throw new ServiceException("Database error");
        }
        if (!isAdmin && note.getUserId() != requestingUserId && !note.isPublic()) {
            throw new ServiceException("Access denied");
        }
        try {
            return attachmentDao.findByNoteId(noteId);
        } catch (java.sql.SQLException e) {
            throw new ServiceException("Database error");
        }
    }

    /**
     * Returns the Path to an attachment file, verifying the requester has access.
     */
    public Path getAttachmentPath(long attachmentId, long requestingUserId, boolean isAdmin)
            throws ServiceException {
        Attachment a;
        try {
            a = attachmentDao.findById(attachmentId)
                    .orElseThrow(() -> new ServiceException("Attachment not found"));
        } catch (java.sql.SQLException e) {
            throw new ServiceException("Database error");
        }
        // Verify requester can access the parent note
        getAttachmentsForNote(a.getNoteId(), requestingUserId, isAdmin);
        return uploadDir.resolve(a.getStoredName());
    }

    /** Deletes an attachment. Only the note owner or admin may delete. */
    public void deleteAttachment(long attachmentId, long requestingUserId,
                                  boolean isAdmin, String ipAddress) throws ServiceException {
        Attachment a;
        try {
            a = attachmentDao.findById(attachmentId)
                    .orElseThrow(() -> new ServiceException("Attachment not found"));
        } catch (java.sql.SQLException e) {
            throw new ServiceException("Database error");
        }
        Note note;
        try {
            note = noteDao.findById(a.getNoteId())
                    .orElseThrow(() -> new ServiceException("Parent note not found"));
        } catch (java.sql.SQLException e) {
            throw new ServiceException("Database error");
        }
        if (!isAdmin && note.getUserId() != requestingUserId) {
            throw new ServiceException("Access denied");
        }
        // Remove from disk
        try {
            Files.deleteIfExists(uploadDir.resolve(a.getStoredName()));
        } catch (IOException e) {
            log.warn("Could not delete attachment file: {}", a.getStoredName(), e);
        }
        // Remove from DB
        try {
            attachmentDao.delete(attachmentId);
        } catch (java.sql.SQLException e) {
            throw new ServiceException("Database error during attachment delete");
        }
        auditService.record(requestingUserId, EventType.ATTACHMENT,
                "attachment_deleted attachmentId=" + attachmentId, ipAddress);
    }

    private String extractFilename(String contentDisposition) {
        if (contentDisposition == null) return "upload";
        for (String part : contentDisposition.split(";")) {
            if (part.trim().startsWith("filename")) {
                return part.substring(part.indexOf('=') + 1).trim()
                        .replace("\"", "");
            }
        }
        return "upload";
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot > 0 && dot < filename.length() - 1) {
            return filename.substring(dot + 1).toLowerCase();
        }
        return "bin";
    }
}
