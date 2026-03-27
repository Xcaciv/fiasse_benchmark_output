package com.loosenotes.service;

import com.loosenotes.dao.AttachmentDao;
import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.RatingDao;
import com.loosenotes.dao.ShareLinkDao;
import com.loosenotes.model.Note;
import com.loosenotes.util.AuditLogger;
import com.loosenotes.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NoteService {
    private static final Logger logger = LoggerFactory.getLogger(NoteService.class);
    private final NoteDao noteDao = new NoteDao();
    private final AttachmentDao attachmentDao = new AttachmentDao();
    private final ShareLinkDao shareLinkDao = new ShareLinkDao();
    private final RatingDao ratingDao = new RatingDao();
    private final AuditLogger auditLogger = AuditLogger.getInstance();

    public Optional<Note> findById(long noteId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return noteDao.findById(conn, noteId);
        } catch (Exception e) {
            logger.error("Error finding note", e);
            return Optional.empty();
        }
    }

    public long createNote(long userId, String title, String content, String visibility, String ip, String sessionId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            Note note = new Note();
            note.setUserId(userId);
            note.setTitle(title);
            note.setContent(content);
            note.setVisibility(visibility != null ? visibility : "PRIVATE");
            long id = noteDao.insert(conn, note);
            auditLogger.log("NOTE_CREATED", userId, String.valueOf(id), ip, "SUCCESS", sessionId, null);
            return id;
        } catch (Exception e) {
            logger.error("Error creating note", e);
            return -1;
        }
    }

    public boolean updateNote(long noteId, long userId, String title, String content, String visibility, String ip, String sessionId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            Optional<Note> noteOpt = noteDao.findById(conn, noteId);
            if (noteOpt.isEmpty() || !noteOpt.get().getUserId().equals(userId)) return false;
            Note note = noteOpt.get();
            String oldVisibility = note.getVisibility();
            note.setTitle(title);
            note.setContent(content);
            note.setVisibility(visibility);
            noteDao.update(conn, note);
            auditLogger.log("NOTE_UPDATED", userId, String.valueOf(noteId), ip, "SUCCESS", sessionId, null);
            if (!oldVisibility.equals(visibility)) {
                auditLogger.log("NOTE_VISIBILITY_CHANGED", userId, String.valueOf(noteId), ip, "SUCCESS", sessionId, "to:" + visibility);
            }
            return true;
        } catch (Exception e) {
            logger.error("Error updating note", e);
            return false;
        }
    }

    public boolean deleteNote(long noteId, long userId, String ip, String sessionId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                Optional<Note> noteOpt = noteDao.findById(conn, noteId);
                if (noteOpt.isEmpty() || !noteOpt.get().getUserId().equals(userId)) {
                    conn.rollback();
                    return false;
                }
                shareLinkDao.deleteByNoteId(conn, noteId);
                attachmentDao.deleteByNoteId(conn, noteId);
                noteDao.delete(conn, noteId, userId);
                conn.commit();
                auditLogger.log("NOTE_DELETED", userId, String.valueOf(noteId), ip, "SUCCESS", sessionId, null);
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            logger.error("Error deleting note", e);
            return false;
        }
    }

    public List<Note> getUserNotes(long userId, int page, int pageSize) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return noteDao.findByUserId(conn, userId, page, pageSize);
        } catch (Exception e) {
            logger.error("Error getting user notes", e);
            return Collections.emptyList();
        }
    }

    public long countUserNotes(long userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return noteDao.countByUserId(conn, userId);
        } catch (Exception e) {
            logger.error("Error counting user notes", e);
            return 0;
        }
    }

    public List<Note> searchPublicNotes(String query, int page, int pageSize) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return noteDao.searchPublic(conn, query, page, pageSize);
        } catch (Exception e) {
            logger.error("Error searching notes", e);
            return Collections.emptyList();
        }
    }

    public List<Note> getTopRatedNotes(int limit) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return noteDao.findTopRated(conn, limit);
        } catch (Exception e) {
            logger.error("Error getting top rated notes", e);
            return Collections.emptyList();
        }
    }
}
