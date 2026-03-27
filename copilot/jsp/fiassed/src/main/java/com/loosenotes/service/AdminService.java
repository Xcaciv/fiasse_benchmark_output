package com.loosenotes.service;

import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.dao.AttachmentDao;
import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.ShareLinkDao;
import com.loosenotes.dao.UserDao;
import com.loosenotes.model.AuditEvent;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import com.loosenotes.util.AuditLogger;
import com.loosenotes.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private final UserDao userDao = new UserDao();
    private final NoteDao noteDao = new NoteDao();
    private final AttachmentDao attachmentDao = new AttachmentDao();
    private final ShareLinkDao shareLinkDao = new ShareLinkDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();
    private final AuditLogger auditLogger = AuditLogger.getInstance();

    public List<User> listUsers(int page, int pageSize) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return userDao.findAll(conn, page, pageSize);
        } catch (Exception e) {
            logger.error("Error listing users", e);
            return Collections.emptyList();
        }
    }

    public long countUsers() {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return userDao.countAll(conn);
        } catch (Exception e) {
            logger.error("Error counting users", e);
            return 0;
        }
    }

    public long countNotes() {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return noteDao.countAll(conn);
        } catch (Exception e) {
            logger.error("Error counting notes", e);
            return 0;
        }
    }

    public List<Note> listAllNotes(int page, int pageSize) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return noteDao.findAll(conn, page, pageSize);
        } catch (Exception e) {
            logger.error("Error listing notes", e);
            return Collections.emptyList();
        }
    }

    public List<AuditEvent> getRecentAuditEvents(int limit) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return auditLogDao.findRecent(conn, limit);
        } catch (Exception e) {
            logger.error("Error getting audit events", e);
            return Collections.emptyList();
        }
    }

    public boolean deleteUser(long targetUserId, long adminUserId, String ip, String sessionId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Delete all notes for the user (cascade)
                List<Note> notes = noteDao.findByUserId(conn, targetUserId, 1, Integer.MAX_VALUE);
                for (Note note : notes) {
                    shareLinkDao.deleteByNoteId(conn, note.getId());
                    attachmentDao.deleteByNoteId(conn, note.getId());
                    noteDao.deleteById(conn, note.getId());
                }
                userDao.deleteById(conn, targetUserId);
                conn.commit();
                auditLogger.log("ADMIN_ACTION", adminUserId, String.valueOf(targetUserId), ip, "SUCCESS", sessionId, "action:delete_user");
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            logger.error("Error deleting user", e);
            return false;
        }
    }

    public boolean reassignNote(long noteId, long newOwnerId, long adminUserId, String ip, String sessionId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            Optional<Note> noteOpt = noteDao.findById(conn, noteId);
            if (noteOpt.isEmpty()) return false;
            Optional<User> newOwnerOpt = userDao.findById(conn, newOwnerId);
            if (newOwnerOpt.isEmpty()) return false;
            Note note = noteOpt.get();
            note.setUserId(newOwnerId);
            noteDao.update(conn, note);
            auditLogger.log("ADMIN_ACTION", adminUserId, String.valueOf(noteId), ip, "SUCCESS", sessionId, "action:reassign_note,new_owner:" + newOwnerId);
            return true;
        } catch (Exception e) {
            logger.error("Error reassigning note", e);
            return false;
        }
    }
}
