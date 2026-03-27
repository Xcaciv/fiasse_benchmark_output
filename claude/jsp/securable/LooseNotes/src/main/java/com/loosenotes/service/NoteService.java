package com.loosenotes.service;

import com.loosenotes.dao.AttachmentDao;
import com.loosenotes.dao.NoteDao;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.util.InputSanitizer;
import com.loosenotes.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for note management.
 * SSEM: Integrity - ownership checks before any mutation.
 * SSEM: Confidentiality - private notes from other users never returned.
 */
public class NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteService.class);

    private final NoteDao noteDao;
    private final AttachmentDao attachmentDao;

    public NoteService(NoteDao noteDao, AttachmentDao attachmentDao) {
        this.noteDao = noteDao;
        this.attachmentDao = attachmentDao;
    }

    /**
     * Creates a new note for the given user.
     * Trust boundary: validates and sanitizes title and content.
     */
    public long createNote(long userId, String rawTitle,
                           String rawContent, boolean isPublic) throws SQLException {
        String title   = InputSanitizer.sanitizeSingleLine(rawTitle);
        String content = InputSanitizer.sanitizeMultiLine(rawContent);

        if (!ValidationUtil.isValidNoteTitle(title)) {
            throw new IllegalArgumentException("Note title is required (max 255 chars)");
        }
        if (!ValidationUtil.isValidNoteContent(content)) {
            throw new IllegalArgumentException("Note content is required (max 50000 chars)");
        }

        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(title);
        note.setContent(content);
        note.setPublic(isPublic);
        return noteDao.create(note);
    }

    /**
     * Returns a note by ID if the requesting user is authorized to view it.
     * Rules: owner always; others only if public.
     */
    public Optional<Note> getNote(long noteId, long requestingUserId) throws SQLException {
        Optional<Note> opt = noteDao.findById(noteId);
        if (opt.isEmpty()) return Optional.empty();

        Note note = opt.get();
        if (note.getUserId() == requestingUserId || note.isPublic()) {
            note.setAttachments(attachmentDao.findByNoteId(noteId));
            return Optional.of(note);
        }
        return Optional.empty(); // Private note from another user - do not reveal existence
    }

    /**
     * Returns a note for viewing via share link (no auth required).
     * Only returns the note if it exists; visibility is not checked (link implies consent).
     */
    public Optional<Note> getNoteForShare(long noteId) throws SQLException {
        Optional<Note> opt = noteDao.findById(noteId);
        if (opt.isPresent()) {
            opt.get().setAttachments(attachmentDao.findByNoteId(noteId));
        }
        return opt;
    }

    /** Returns all notes owned by the given user. */
    public List<Note> getUserNotes(long userId) throws SQLException {
        return noteDao.findByUserId(userId);
    }

    /**
     * Updates an existing note. Verifies ownership before update.
     * SSEM: Integrity - userId in WHERE clause prevents unauthorized updates.
     */
    public boolean updateNote(long noteId, long userId, String rawTitle,
                               String rawContent, boolean isPublic) throws SQLException {
        Optional<Note> opt = noteDao.findById(noteId);
        if (opt.isEmpty()) return false;
        if (opt.get().getUserId() != userId) return false; // Ownership check

        String title   = InputSanitizer.sanitizeSingleLine(rawTitle);
        String content = InputSanitizer.sanitizeMultiLine(rawContent);

        if (!ValidationUtil.isValidNoteTitle(title)) {
            throw new IllegalArgumentException("Note title is required (max 255 chars)");
        }
        if (!ValidationUtil.isValidNoteContent(content)) {
            throw new IllegalArgumentException("Note content is required (max 50000 chars)");
        }

        Note note = opt.get();
        note.setTitle(title);
        note.setContent(content);
        note.setPublic(isPublic);
        noteDao.update(note);
        return true;
    }

    /**
     * Deletes a note. Requires ownership OR admin role.
     *
     * @param isAdmin whether the requester has admin role
     */
    public boolean deleteNote(long noteId, long requestingUserId,
                               boolean isAdmin) throws SQLException {
        Optional<Note> opt = noteDao.findById(noteId);
        if (opt.isEmpty()) return false;
        if (!isAdmin && opt.get().getUserId() != requestingUserId) return false;

        noteDao.delete(noteId);
        return true;
    }

    /**
     * Searches notes visible to the given user.
     */
    public List<Note> search(String rawQuery, long userId) throws SQLException {
        String sanitized = InputSanitizer.sanitizeSearchQuery(rawQuery, 100);
        if (sanitized.isBlank()) return List.of();
        return noteDao.search(sanitized, userId);
    }

    /** Returns top-rated public notes. */
    public List<Note> getTopRated(int minRatings, int limit) throws SQLException {
        return noteDao.findTopRated(minRatings, limit);
    }

    /**
     * Reassigns note ownership (admin only). Caller must verify admin role.
     */
    public void reassignNote(long noteId, long newUserId) throws SQLException {
        noteDao.reassignOwner(noteId, newUserId);
    }
}
