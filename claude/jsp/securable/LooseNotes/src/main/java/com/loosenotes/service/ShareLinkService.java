package com.loosenotes.service;

import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.ShareLinkDao;
import com.loosenotes.model.AuditLog.EventType;
import com.loosenotes.model.Note;
import com.loosenotes.model.ShareLink;
import com.loosenotes.util.SecureTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Business logic for generating, retrieving, and revoking share links.
 *
 * SSEM notes:
 * - Authenticity: tokens are 256-bit cryptographically random values (SecureTokenUtil).
 * - Integrity: only the note owner may generate or revoke links.
 * - Accountability: share events are audited.
 */
public class ShareLinkService {

    private static final Logger log = LoggerFactory.getLogger(ShareLinkService.class);

    private final ShareLinkDao shareLinkDao;
    private final NoteDao noteDao;
    private final AuditService auditService;

    public ShareLinkService(ShareLinkDao shareLinkDao, NoteDao noteDao, AuditService auditService) {
        this.shareLinkDao = shareLinkDao;
        this.noteDao      = noteDao;
        this.auditService = auditService;
    }

    /**
     * Generates a new share link for a note, revoking any existing one.
     * Only the note owner may call this.
     */
    public ShareLink generateLink(long noteId, long requestingUserId, String ipAddress)
            throws ServiceException, SQLException {

        Note note = noteDao.findById(noteId)
                .orElseThrow(() -> new ServiceException("Note not found"));
        if (note.getUserId() != requestingUserId) {
            throw new ServiceException("Access denied");
        }

        // Revoke existing link so only one active link per note exists
        shareLinkDao.deleteByNoteId(noteId);

        ShareLink link = new ShareLink();
        link.setNoteId(noteId);
        link.setToken(SecureTokenUtil.generate());
        shareLinkDao.insert(link);

        auditService.record(requestingUserId, EventType.SHARE,
                "share_link_created noteId=" + noteId, ipAddress);
        return link;
    }

    /**
     * Returns the note associated with a share token.
     * Token lookup succeeds regardless of note visibility (the link IS the access grant).
     */
    public Note getNoteByToken(String token) throws ServiceException, SQLException {
        ShareLink link = shareLinkDao.findByToken(token)
                .orElseThrow(() -> new ServiceException("Share link not found or revoked"));
        return noteDao.findById(link.getNoteId())
                .orElseThrow(() -> new ServiceException("Note not found"));
    }

    /** Returns the current share link for a note (owner view). */
    public Optional<ShareLink> getLinkForNote(long noteId, long requestingUserId)
            throws ServiceException, SQLException {
        Note note = noteDao.findById(noteId)
                .orElseThrow(() -> new ServiceException("Note not found"));
        if (note.getUserId() != requestingUserId) {
            throw new ServiceException("Access denied");
        }
        return shareLinkDao.findByNoteId(noteId);
    }

    /**
     * Revokes the share link for a note. Only the note owner may call this.
     */
    public void revokeLink(long noteId, long requestingUserId, String ipAddress)
            throws ServiceException, SQLException {
        Note note = noteDao.findById(noteId)
                .orElseThrow(() -> new ServiceException("Note not found"));
        if (note.getUserId() != requestingUserId) {
            throw new ServiceException("Access denied");
        }
        shareLinkDao.deleteByNoteId(noteId);
        auditService.record(requestingUserId, EventType.SHARE,
                "share_link_revoked noteId=" + noteId, ipAddress);
    }
}
