package com.loosenotes.service;

import com.loosenotes.dao.AttachmentDao;
import com.loosenotes.dao.NoteDao;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.util.AuditLogger;
import com.loosenotes.util.DatabaseManager;
import com.loosenotes.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AttachmentService {
    private static final Logger logger = LoggerFactory.getLogger(AttachmentService.class);
    private static final int MAX_ATTACHMENTS_PER_NOTE = 5;
    private final AttachmentDao attachmentDao = new AttachmentDao();
    private final NoteDao noteDao = new NoteDao();
    private final AuditLogger auditLogger = AuditLogger.getInstance();

    public String uploadAttachment(long noteId, long userId, Part part, String ip, String sessionId) throws IOException {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            Optional<Note> noteOpt = noteDao.findById(conn, noteId);
            if (noteOpt.isEmpty() || !noteOpt.get().getUserId().equals(userId)) {
                return "Access denied";
            }
            if (attachmentDao.countByNoteId(conn, noteId) >= MAX_ATTACHMENTS_PER_NOTE) {
                return "Maximum attachments reached (5)";
            }
            if (part.getSize() > FileUtils.getMaxFileSize()) {
                return "File too large (max 10MB)";
            }
            String detectedMime;
            try (InputStream is = part.getInputStream()) {
                detectedMime = FileUtils.detectMimeType(is);
            }
            if (!FileUtils.isAllowedMimeType(detectedMime)) {
                return "File type not allowed";
            }
            String originalName = FileUtils.sanitizeFilename(part.getSubmittedFileName());
            String storedName = FileUtils.generateStoredFilename(originalName);
            Path uploadDir = FileUtils.getUploadDirectory();
            Path targetPath = uploadDir.resolve(storedName);
            try (InputStream is = part.getInputStream()) {
                Files.copy(is, targetPath);
            }
            Attachment attachment = new Attachment();
            attachment.setNoteId(noteId);
            attachment.setOriginalFilename(originalName);
            attachment.setStoredFilename(storedName);
            attachment.setContentType(detectedMime);
            attachment.setFileSize(part.getSize());
            attachmentDao.insert(conn, attachment);
            auditLogger.log("FILE_UPLOADED", userId, String.valueOf(noteId), ip, "SUCCESS", sessionId, "file:" + originalName);
            return null; // null = success
        } catch (Exception e) {
            logger.error("Error uploading attachment", e);
            return "Upload failed";
        }
    }

    public Optional<Attachment> findById(long attachmentId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return attachmentDao.findById(conn, attachmentId);
        } catch (Exception e) {
            logger.error("Error finding attachment", e);
            return Optional.empty();
        }
    }

    public List<Attachment> findByNoteId(long noteId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return attachmentDao.findByNoteId(conn, noteId);
        } catch (Exception e) {
            logger.error("Error finding attachments", e);
            return Collections.emptyList();
        }
    }

    public boolean canAccess(long attachmentId, Long userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            Optional<Attachment> attOpt = attachmentDao.findById(conn, attachmentId);
            if (attOpt.isEmpty()) return false;
            Optional<Note> noteOpt = noteDao.findById(conn, attOpt.get().getNoteId());
            if (noteOpt.isEmpty()) return false;
            Note note = noteOpt.get();
            return note.isPublic() || (userId != null && note.getUserId().equals(userId));
        } catch (Exception e) {
            logger.error("Error checking attachment access", e);
            return false;
        }
    }
}
