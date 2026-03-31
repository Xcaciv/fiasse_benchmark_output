package com.loosenotes.service;

import com.loosenotes.dao.NoteDao;
import com.loosenotes.model.AuditLog.EventType;
import com.loosenotes.model.Note;
import com.loosenotes.model.Note.Visibility;
import com.loosenotes.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Business logic for note lifecycle (create, read, update, delete, search).
 *
 * SSEM / ASVS alignment:
 * - Integrity: ownership verified before mutating operations (Derived Integrity).
 * - Confidentiality: private notes only returned to owner or admin.
 * - Accountability: all mutations audited.
 */
public class NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteService.class);

    private final NoteDao noteDao;
    private final AuditService auditService;
    private final int maxTitleLength;
    private final int maxContentBytes;

    public NoteService(NoteDao noteDao, AuditService auditService,
                       int maxTitleLength, int maxContentBytes) {
        this.noteDao         = noteDao;
        this.auditService    = auditService;
        this.maxTitleLength  = maxTitleLength;
        this.maxContentBytes = maxContentBytes;
    }

    /**
     * Returns a note if the requesting user is allowed to view it.
     * Owners can see their own notes; others can only see public notes.
     */
    public Note getNoteForUser(long noteId, long requestingUserId, boolean isAdmin)
            throws ServiceException, SQLException {
        Note note = noteDao.findById(noteId)
                .orElseThrow(() -> new ServiceException("Note not found"));
        if (!isAdmin && note.getUserId() != requestingUserId && !note.isPublic()) {
            throw new ServiceException("Access denied");
        }
        return note;
    }

    /** Returns all notes owned by a user. */
    public List<Note> getNotesForOwner(long userId) throws SQLException {
        return noteDao.findByUserId(userId);
    }

    /** Full-text search respecting visibility rules. */
    public List<Note> search(String query, long requestingUserId) throws ServiceException, SQLException {
        String trimmed = ValidationUtil.trimOrNull(query);
        if (trimmed == null || trimmed.length() < 1) {
            throw new ServiceException("Search query must not be empty");
        }
        return noteDao.search(trimmed, requestingUserId);
    }

    /** Returns top-rated public notes with at least 3 ratings. */
    public List<Note> getTopRated() throws SQLException {
        return noteDao.findTopRated(3, 20);
    }

    /**
     * Creates a new note owned by the given user.
     */
    public long createNote(long ownerId, String title, String content,
                           Visibility visibility, String ipAddress)
            throws ServiceException, SQLException {

        validateTitleAndContent(title, content);
        Note note = new Note();
        note.setUserId(ownerId);
        note.setTitle(title.strip());
        note.setContent(content);
        note.setVisibility(visibility != null ? visibility : Visibility.PRIVATE);

        long id = noteDao.insert(note);
        auditService.record(ownerId, EventType.NOTE,
                "note_created noteId=" + id, ipAddress);
        return id;
    }

    /**
     * Updates a note. Only the owner (or admin) may edit.
     */
    public void updateNote(long noteId, long requestingUserId, boolean isAdmin,
                           String title, String content, Visibility visibility,
                           String ipAddress) throws ServiceException, SQLException {

        Note note = noteDao.findById(noteId)
                .orElseThrow(() -> new ServiceException("Note not found"));
        if (!isAdmin && note.getUserId() != requestingUserId) {
            throw new ServiceException("Access denied");
        }
        validateTitleAndContent(title, content);
        note.setTitle(title.strip());
        note.setContent(content);
        note.setVisibility(visibility != null ? visibility : note.getVisibility());

        noteDao.update(note);
        auditService.record(requestingUserId, EventType.NOTE,
                "note_updated noteId=" + noteId, ipAddress);
    }

    /**
     * Deletes a note. Only the owner or an admin may delete.
     * Cascades to attachments, ratings, and share links via FK ON DELETE CASCADE.
     */
    public void deleteNote(long noteId, long requestingUserId, boolean isAdmin, String ipAddress)
            throws ServiceException, SQLException {

        Note note = noteDao.findById(noteId)
                .orElseThrow(() -> new ServiceException("Note not found"));
        if (!isAdmin && note.getUserId() != requestingUserId) {
            throw new ServiceException("Access denied");
        }
        noteDao.delete(noteId);
        auditService.record(requestingUserId, EventType.NOTE,
                "note_deleted noteId=" + noteId, ipAddress);
    }

    /**
     * Reassigns a note to a new owner (admin only).
     */
    public void reassignNote(long noteId, long newOwnerId, long adminUserId, String ipAddress)
            throws ServiceException, SQLException {

        if (noteDao.findById(noteId).isEmpty()) {
            throw new ServiceException("Note not found");
        }
        noteDao.reassignOwner(noteId, newOwnerId);
        auditService.record(adminUserId, EventType.ADMIN,
                "note_reassigned noteId=" + noteId + " newOwner=" + newOwnerId, ipAddress);
    }

    /** Returns total note count (admin dashboard). */
    public int countAll() throws SQLException {
        return noteDao.countAll();
    }

    private void validateTitleAndContent(String title, String content) throws ServiceException {
        if (!ValidationUtil.isValidNoteTitle(title, maxTitleLength)) {
            throw new ServiceException("Title is required and must be ≤ " + maxTitleLength + " characters");
        }
        if (!ValidationUtil.isValidNoteContent(content, maxContentBytes)) {
            throw new ServiceException("Content is required and must be ≤ " + maxContentBytes + " bytes");
        }
    }
}
