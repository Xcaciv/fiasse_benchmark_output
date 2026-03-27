package com.loosenotes.service;

import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.ShareLinkDao;
import com.loosenotes.model.Note;
import com.loosenotes.model.ShareLink;
import com.loosenotes.util.SecureTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Manages share link creation, revocation, and validation.
 * SSEM: Authenticity - cryptographically random tokens, stored as hashes.
 * SSEM: Integrity - ownership verified before link generation.
 * SSEM: Confidentiality - raw token returned once; only hash persisted.
 */
public class ShareLinkService {

    private static final Logger log = LoggerFactory.getLogger(ShareLinkService.class);

    private final ShareLinkDao shareLinkDao;
    private final NoteDao noteDao;

    public ShareLinkService(ShareLinkDao shareLinkDao, NoteDao noteDao) {
        this.shareLinkDao = shareLinkDao;
        this.noteDao = noteDao;
    }

    /**
     * Generates a new share link for the given note.
     * The raw token is returned to the caller for inclusion in the share URL.
     * Only the SHA-256 hash is stored in the database.
     *
     * @param noteId  note to share
     * @param ownerId user requesting the share link
     * @return raw token (include in URL), or empty if note not found/unauthorized
     */
    public Optional<String> generateLink(long noteId, long ownerId) throws SQLException {
        Optional<Note> note = noteDao.findById(noteId);
        if (note.isEmpty() || note.get().getUserId() != ownerId) {
            return Optional.empty();
        }

        String rawToken  = SecureTokenUtil.generateToken();
        String tokenHash = SecureTokenUtil.hashToken(rawToken);

        ShareLink link = new ShareLink();
        link.setNoteId(noteId);
        link.setToken(tokenHash); // Only hash goes to DB
        link.setActive(true);
        shareLinkDao.create(link);

        log.info("Share link generated for noteId={} by userId={}", noteId, ownerId);
        return Optional.of(rawToken); // Raw token returned to user
    }

    /**
     * Revokes all share links for a note.
     * Caller must verify ownership before calling.
     */
    public void revokeLinks(long noteId, long ownerId) throws SQLException {
        Optional<Note> note = noteDao.findById(noteId);
        if (note.isEmpty() || note.get().getUserId() != ownerId) {
            throw new SecurityException("Note not found or access denied");
        }
        shareLinkDao.deactivateByNoteId(noteId);
        log.info("Share links revoked for noteId={} by userId={}", noteId, ownerId);
    }

    /**
     * Resolves a raw token from a URL to the associated note ID.
     * Computes the hash and looks it up.
     *
     * @param rawToken URL token provided by the visitor
     * @return noteId if the link is active, or empty if invalid/inactive
     */
    public Optional<Long> resolveToken(String rawToken) throws SQLException {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        String tokenHash = SecureTokenUtil.hashToken(rawToken);
        Optional<ShareLink> link = shareLinkDao.findActiveByToken(tokenHash);
        return link.map(ShareLink::getNoteId);
    }

    /** Returns the active share link record for a note (for display to owner). */
    public Optional<ShareLink> getActiveLinkForNote(long noteId) throws SQLException {
        return shareLinkDao.findActiveByNoteId(noteId);
    }
}
