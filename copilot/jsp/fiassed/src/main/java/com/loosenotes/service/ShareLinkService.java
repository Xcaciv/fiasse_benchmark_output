package com.loosenotes.service;

import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.ShareLinkDao;
import com.loosenotes.model.Note;
import com.loosenotes.model.ShareLink;
import com.loosenotes.util.AuditLogger;
import com.loosenotes.util.DatabaseManager;
import com.loosenotes.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Optional;

public class ShareLinkService {
    private static final Logger logger = LoggerFactory.getLogger(ShareLinkService.class);
    private final ShareLinkDao shareLinkDao = new ShareLinkDao();
    private final NoteDao noteDao = new NoteDao();
    private final AuditLogger auditLogger = AuditLogger.getInstance();

    public Optional<String> generateShareLink(long noteId, long userId, String ip, String sessionId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            Optional<Note> noteOpt = noteDao.findById(conn, noteId);
            if (noteOpt.isEmpty() || !noteOpt.get().getUserId().equals(userId)) return Optional.empty();
            // Revoke existing
            shareLinkDao.deleteByNoteId(conn, noteId);
            String token = SecurityUtils.generateSecureToken();
            ShareLink sl = new ShareLink();
            sl.setNoteId(noteId);
            sl.setToken(token);
            shareLinkDao.insert(conn, sl);
            auditLogger.log("SHARE_LINK_GENERATED", userId, String.valueOf(noteId), ip, "SUCCESS", sessionId, null);
            return Optional.of(token);
        } catch (Exception e) {
            logger.error("Error generating share link", e);
            return Optional.empty();
        }
    }

    public Optional<Note> findNoteByToken(String token, String ip) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            Optional<ShareLink> slOpt = shareLinkDao.findByToken(conn, token);
            if (slOpt.isEmpty()) return Optional.empty();
            Optional<Note> noteOpt = noteDao.findById(conn, slOpt.get().getNoteId());
            noteOpt.ifPresent(note ->
                auditLogger.log("SHARE_LINK_ACCESSED", null, String.valueOf(note.getId()), ip, "SUCCESS", null,
                    "token_hash:" + SecurityUtils.sha256Hex(token))
            );
            return noteOpt;
        } catch (Exception e) {
            logger.error("Error finding note by share token", e);
            return Optional.empty();
        }
    }

    public boolean revokeShareLink(long noteId, long userId, String ip, String sessionId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            Optional<Note> noteOpt = noteDao.findById(conn, noteId);
            if (noteOpt.isEmpty() || !noteOpt.get().getUserId().equals(userId)) return false;
            shareLinkDao.deleteByNoteId(conn, noteId);
            auditLogger.log("SHARE_LINK_REVOKED", userId, String.valueOf(noteId), ip, "SUCCESS", sessionId, null);
            return true;
        } catch (Exception e) {
            logger.error("Error revoking share link", e);
            return false;
        }
    }

    public Optional<ShareLink> findByNoteId(long noteId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return shareLinkDao.findByNoteId(conn, noteId);
        } catch (Exception e) {
            logger.error("Error finding share link", e);
            return Optional.empty();
        }
    }
}
