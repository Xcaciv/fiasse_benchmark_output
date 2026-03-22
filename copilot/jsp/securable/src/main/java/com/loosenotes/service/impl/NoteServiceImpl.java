package com.loosenotes.service.impl;

import com.loosenotes.audit.AuditLogger;
import com.loosenotes.dao.AttachmentDao;
import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.RatingDao;
import com.loosenotes.dao.ShareLinkDao;
import com.loosenotes.model.*;
import com.loosenotes.service.FileService;
import com.loosenotes.service.NoteService;
import com.loosenotes.service.ServiceException;
import com.loosenotes.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public final class NoteServiceImpl implements NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SHARE_TOKEN_BYTES = 24;

    private final NoteDao       noteDao;
    private final AttachmentDao attachmentDao;
    private final RatingDao     ratingDao;
    private final ShareLinkDao  shareLinkDao;
    private final FileService   fileService;
    private final AuditLogger   auditLogger;

    public NoteServiceImpl(NoteDao noteDao, AttachmentDao attachmentDao,
                           RatingDao ratingDao, ShareLinkDao shareLinkDao,
                           FileService fileService, AuditLogger auditLogger) {
        this.noteDao       = noteDao;
        this.attachmentDao = attachmentDao;
        this.ratingDao     = ratingDao;
        this.shareLinkDao  = shareLinkDao;
        this.fileService   = fileService;
        this.auditLogger   = auditLogger;
    }

    @Override
    public Note createNote(long userId, String title, String content,
                           Note.Visibility visibility) {
        String cleanTitle   = ValidationUtil.validateTitle(title);
        String cleanContent = ValidationUtil.validateContent(content);
        Note.Visibility vis = visibility != null ? visibility : Note.Visibility.PRIVATE;

        Note note = new Note(0, userId, cleanTitle, cleanContent, vis,
                LocalDateTime.now(), LocalDateTime.now(), null);
        long id = noteDao.insert(note);

        auditLogger.log(userId, null, "NOTE_CREATE", "NOTE",
                String.valueOf(id), null, "SUCCESS", null);
        return noteDao.findById(id).orElseThrow(() ->
                new ServiceException(ServiceException.ErrorCode.NOT_FOUND, "Note not found after insert"));
    }

    @Override
    public Optional<Note> findById(long noteId) {
        return noteDao.findById(noteId);
    }

    @Override
    public List<Note> findByUser(long userId) {
        return noteDao.findByUserId(userId);
    }

    @Override
    public void updateNote(long noteId, long requestingUserId,
                           String title, String content, Note.Visibility visibility) {
        Note existing = requireNoteOwner(noteId, requestingUserId);
        String cleanTitle   = ValidationUtil.validateTitle(title);
        String cleanContent = ValidationUtil.validateContent(content);
        Note.Visibility vis = visibility != null ? visibility : existing.getVisibility();

        Note updated = new Note(noteId, requestingUserId, cleanTitle, cleanContent,
                vis, existing.getCreatedAt(), LocalDateTime.now(), null);
        noteDao.update(updated);
        auditLogger.log(requestingUserId, null, "NOTE_UPDATE", "NOTE",
                String.valueOf(noteId), null, "SUCCESS", null);
    }

    @Override
    public void deleteNote(long noteId, long requestingUserId, boolean isAdmin) {
        Note note = noteDao.findById(noteId)
                .orElseThrow(() -> new ServiceException(
                        ServiceException.ErrorCode.NOT_FOUND, "Note not found"));
        if (!isAdmin && note.getUserId() != requestingUserId) {
            throw new ServiceException(ServiceException.ErrorCode.UNAUTHORIZED,
                    "Not authorized to delete this note");
        }
        // Delete physical attachments before DB record (Resilience: avoid orphans)
        attachmentDao.findByNoteId(noteId).forEach(a ->
                fileService.delete(a.getStoredFilename()));

        noteDao.delete(noteId);
        auditLogger.log(requestingUserId, null, "NOTE_DELETE", "NOTE",
                String.valueOf(noteId), null, "SUCCESS", null);
    }

    @Override
    public List<Note> search(String keyword, long requestingUserId) {
        if (keyword == null || keyword.trim().isEmpty()) return List.of();
        String clean = ValidationUtil.requireNonBlank(keyword, "keyword", 200);
        return noteDao.searchNotes(clean, requestingUserId);
    }

    @Override
    public List<Note> findTopRated(int limit) {
        return noteDao.findTopRated(3, limit > 0 ? limit : 20);
    }

    // --- Attachments ---

    @Override
    public Attachment addAttachment(long noteId, long requestingUserId,
                                    String originalFilename, InputStream data, long fileSize) {
        requireNoteOwner(noteId, requestingUserId);
        ValidationUtil.validateFileExtension(originalFilename);

        String storedName = fileService.store(originalFilename, data, fileSize);
        String mimeType   = guessMimeType(originalFilename);
        Attachment a = new Attachment(0, noteId, originalFilename,
                storedName, fileSize, mimeType, LocalDateTime.now());
        long id = attachmentDao.insert(a);

        auditLogger.log(requestingUserId, null, "ATTACHMENT_UPLOAD", "ATTACHMENT",
                String.valueOf(id), null, "SUCCESS", "file=" + originalFilename);
        return attachmentDao.findById(id).orElseThrow();
    }

    @Override
    public Optional<Attachment> findAttachment(long attachmentId) {
        return attachmentDao.findById(attachmentId);
    }

    @Override
    public void deleteAttachment(long attachmentId, long requestingUserId, boolean isAdmin) {
        Attachment a = attachmentDao.findById(attachmentId)
                .orElseThrow(() -> new ServiceException(
                        ServiceException.ErrorCode.NOT_FOUND, "Attachment not found"));
        Note note = noteDao.findById(a.getNoteId())
                .orElseThrow(() -> new ServiceException(
                        ServiceException.ErrorCode.NOT_FOUND, "Note not found"));
        if (!isAdmin && note.getUserId() != requestingUserId) {
            throw new ServiceException(ServiceException.ErrorCode.UNAUTHORIZED,
                    "Not authorized to delete this attachment");
        }
        fileService.delete(a.getStoredFilename());
        attachmentDao.delete(attachmentId);
        auditLogger.log(requestingUserId, null, "ATTACHMENT_DELETE", "ATTACHMENT",
                String.valueOf(attachmentId), null, "SUCCESS", null);
    }

    @Override
    public List<Attachment> getAttachments(long noteId) {
        return attachmentDao.findByNoteId(noteId);
    }

    // --- Share links ---

    @Override
    public ShareLink generateShareLink(long noteId, long requestingUserId) {
        requireNoteOwner(noteId, requestingUserId);
        shareLinkDao.deleteByNoteId(noteId); // revoke old link

        byte[] bytes = new byte[SHARE_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        ShareLink link = new ShareLink(0, noteId, token, LocalDateTime.now());
        shareLinkDao.insert(link);
        auditLogger.log(requestingUserId, null, "SHARE_LINK_GENERATE", "NOTE",
                String.valueOf(noteId), null, "SUCCESS", null);
        return shareLinkDao.findByNoteId(noteId).orElseThrow();
    }

    @Override
    public void revokeShareLink(long noteId, long requestingUserId) {
        requireNoteOwner(noteId, requestingUserId);
        shareLinkDao.deleteByNoteId(noteId);
        auditLogger.log(requestingUserId, null, "SHARE_LINK_REVOKE", "NOTE",
                String.valueOf(noteId), null, "SUCCESS", null);
    }

    @Override
    public Optional<ShareLink> findShareLink(long noteId) {
        return shareLinkDao.findByNoteId(noteId);
    }

    @Override
    public Optional<Note> findNoteByShareToken(String token) {
        return shareLinkDao.findByToken(token)
                .flatMap(link -> noteDao.findById(link.getNoteId()));
    }

    // --- Ratings ---

    @Override
    public Rating addOrUpdateRating(long noteId, long userId, int stars, String comment) {
        ValidationUtil.validateStars(String.valueOf(stars));
        String cleanComment = ValidationUtil.validateComment(comment);

        Optional<Rating> existing = ratingDao.findByNoteAndUser(noteId, userId);
        if (existing.isPresent()) {
            Rating updated = new Rating(existing.get().getId(), noteId, userId,
                    stars, cleanComment, existing.get().getCreatedAt(),
                    LocalDateTime.now(), null);
            ratingDao.update(updated);
            auditLogger.log(userId, null, "RATING_UPDATE", "NOTE",
                    String.valueOf(noteId), null, "SUCCESS", "stars=" + stars);
            return ratingDao.findById(existing.get().getId()).orElseThrow();
        } else {
            Rating newRating = new Rating(0, noteId, userId, stars, cleanComment,
                    LocalDateTime.now(), LocalDateTime.now(), null);
            long id = ratingDao.insert(newRating);
            auditLogger.log(userId, null, "RATING_CREATE", "NOTE",
                    String.valueOf(noteId), null, "SUCCESS", "stars=" + stars);
            return ratingDao.findById(id).orElseThrow();
        }
    }

    @Override
    public List<Rating> getRatings(long noteId) {
        return ratingDao.findByNoteId(noteId);
    }

    @Override
    public double getAverageRating(long noteId) {
        return ratingDao.getAverageForNote(noteId);
    }

    @Override
    public int getRatingCount(long noteId) {
        return ratingDao.countForNote(noteId);
    }

    @Override
    public Optional<Rating> getUserRating(long noteId, long userId) {
        return ratingDao.findByNoteAndUser(noteId, userId);
    }

    // --- Admin ---

    @Override
    public void reassignNote(long noteId, long newOwnerId, long adminId) {
        noteDao.findById(noteId)
                .orElseThrow(() -> new ServiceException(
                        ServiceException.ErrorCode.NOT_FOUND, "Note not found"));
        noteDao.changeOwner(noteId, newOwnerId);
        auditLogger.log(adminId, null, "NOTE_REASSIGN", "NOTE",
                String.valueOf(noteId), null, "SUCCESS",
                "newOwnerId=" + newOwnerId);
    }

    @Override
    public int countAll() {
        return noteDao.countAll();
    }

    // --- Private helpers ---

    /** Verifies ownership; throws UNAUTHORIZED if note doesn't belong to requestingUserId. */
    private Note requireNoteOwner(long noteId, long requestingUserId) {
        Note note = noteDao.findById(noteId)
                .orElseThrow(() -> new ServiceException(
                        ServiceException.ErrorCode.NOT_FOUND, "Note not found"));
        if (note.getUserId() != requestingUserId) {
            throw new ServiceException(ServiceException.ErrorCode.UNAUTHORIZED,
                    "Not authorized to modify this note");
        }
        return note;
    }

    private String guessMimeType(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf"))              return "application/pdf";
        if (lower.endsWith(".doc"))              return "application/msword";
        if (lower.endsWith(".docx"))             return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".txt"))              return "text/plain";
        if (lower.endsWith(".png"))              return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
