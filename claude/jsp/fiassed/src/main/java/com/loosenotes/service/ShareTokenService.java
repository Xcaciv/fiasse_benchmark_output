package com.loosenotes.service;

import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.ShareLinkDao;
import com.loosenotes.model.AuditEvent;
import com.loosenotes.model.Note;
import com.loosenotes.model.ShareLink;
import com.loosenotes.util.TokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * Application-layer service for share-link lifecycle management (F-08).
 *
 * <p><strong>Token security model:</strong>
 * <ul>
 *   <li>Raw tokens are generated via {@link TokenUtil#generateToken128Bit()} and
 *       returned to the caller exactly once — they are NEVER stored in the
 *       database.  Only a cryptographic hash is persisted so that a database
 *       compromise does not expose valid tokens.</li>
 *   <li>Token hashes are looked up at resolution time; the raw value is
 *       discarded immediately after hashing.</li>
 *   <li>Audit events reference the link's surrogate database ID, not the raw
 *       token, so audit logs cannot be used to reconstruct a valid share URL.</li>
 *   <li>A per-user active-link cap ({@link #MAX_ACTIVE_LINKS_PER_USER}) limits
 *       the blast radius of a compromised account.</li>
 * </ul>
 */
public class ShareTokenService {

    private static final Logger log = LoggerFactory.getLogger(ShareTokenService.class);

    /** Maximum number of non-revoked share links a single user may hold. */
    public static final int MAX_ACTIVE_LINKS_PER_USER = 20;

    private final ShareLinkDao shareLinkDao;
    private final NoteDao noteDao;
    private final AuditService auditService;

    /**
     * @param shareLinkDao DAO for share-link persistence; must not be {@code null}
     * @param noteDao      DAO for note lookups; must not be {@code null}
     * @param auditService audit sink; must not be {@code null}
     */
    public ShareTokenService(ShareLinkDao shareLinkDao, NoteDao noteDao, AuditService auditService) {
        if (shareLinkDao == null) throw new IllegalArgumentException("shareLinkDao must not be null");
        if (noteDao == null) throw new IllegalArgumentException("noteDao must not be null");
        if (auditService == null) throw new IllegalArgumentException("auditService must not be null");
        this.shareLinkDao = shareLinkDao;
        this.noteDao = noteDao;
        this.auditService = auditService;
    }

    // =========================================================================
    // Create
    // =========================================================================

    /**
     * Creates a new share link for a note.
     *
     * <p>The caller receives the raw (un-hashed) token exactly once.  The
     * persistence layer stores only the hash so the raw value is never
     * retrievable from the database.
     *
     * @param noteId    note to share (requesting user must own it)
     * @param userId    authenticated owner's ID
     * @param ipAddress caller IP for audit trail
     * @return the raw 128-bit token string (URL-safe base64 or hex, per
     *         {@link TokenUtil#generateToken128Bit()}) — only returned once
     * @throws ServiceException NOT_FOUND, ACCESS_DENIED, or VALIDATION (quota)
     */
    public String createShareLink(Long noteId, Long userId, String ipAddress)
            throws ServiceException {

        // Verify note exists and caller owns it.
        Note note = requireNote(noteId);
        requireOwnership(note, userId);

        // Enforce per-user active-link quota.
        int activeCount = shareLinkDao.countActiveByUserId(userId);
        if (activeCount >= MAX_ACTIVE_LINKS_PER_USER) {
            throw new ServiceException("VALIDATION",
                    "Maximum number of active share links (" + MAX_ACTIVE_LINKS_PER_USER
                            + ") reached. Revoke an existing link before creating a new one.");
        }

        // Generate token — only the hash is stored.
        String rawToken = TokenUtil.generateToken128Bit();
        String tokenHash = TokenUtil.hashToken(rawToken);

        ShareLink link = new ShareLink();
        link.setNoteId(noteId);
        link.setTokenHash(tokenHash);
        link.setCreatedAt(Instant.now());
        // revokedAt is null (active).

        ShareLink created = shareLinkDao.create(link);

        // Audit: log link DB ID, never the raw token or hash.
        auditService.recordEvent(AuditEvent.builder()
                .action("SHARE_LINK_CREATED")
                .subjectId(String.valueOf(userId))
                .objectId(String.valueOf(noteId))
                .metadata("linkId=" + created.getId())
                .ipAddress(ipAddress)
                .outcome("SUCCESS")
                .build());

        log.info("Share link created. linkId={} noteId={} userId={} ip={}",
                created.getId(), noteId, userId, ipAddress);

        // Return raw token — caller is responsible for immediate delivery to end-user.
        return rawToken;
    }

    // =========================================================================
    // Resolve (anonymous access via token)
    // =========================================================================

    /**
     * Resolves a raw share-link token to its associated {@link Note}.
     *
     * <p>The token is hashed immediately upon receipt; the raw value is not
     * retained beyond this method's stack frame.  Audit events reference only
     * the link's database ID.
     *
     * <p>The returned {@link Note} exposes only the fields necessary for
     * display (title, content).  Internal owner IDs are present on the object
     * but callers must take care not to expose them in share-view responses.
     *
     * @param token     raw share token from the URL
     * @param ipAddress caller IP for audit trail
     * @return the {@link Note} associated with the share link
     * @throws ServiceException NOT_FOUND if the token is unknown, or
     *                          ACCESS_DENIED if the link has been revoked
     */
    public Note resolveShareLink(String token, String ipAddress) throws ServiceException {
        if (token == null || token.isEmpty()) {
            throw new ServiceException("NOT_FOUND", "Share link not found.");
        }

        // Hash immediately — do not log or store the raw token.
        String tokenHash = TokenUtil.hashToken(token);

        ShareLink link = shareLinkDao.findByTokenHash(tokenHash);
        if (link == null) {
            // Same exception for "not found" and "revoked" — prevents oracle attacks.
            log.warn("Share link lookup failed (not found). ip={}", ipAddress);
            throw new ServiceException("NOT_FOUND", "Share link not found or has been revoked.");
        }

        if (link.getRevokedAt() != null) {
            log.warn("Share link lookup failed (revoked). linkId={} ip={}", link.getId(), ipAddress);
            // Same message as "not found" to prevent distinguishing revoked from unknown.
            throw new ServiceException("NOT_FOUND", "Share link not found or has been revoked.");
        }

        Note note = noteDao.findById(link.getNoteId());
        if (note == null) {
            log.error("Share link references non-existent note. linkId={} noteId={}",
                    link.getId(), link.getNoteId());
            throw new ServiceException("NOT_FOUND", "Share link not found or has been revoked.");
        }

        // Audit: reference link ID, NOT the raw token or hash.
        auditService.recordEvent(AuditEvent.builder()
                .action("SHARE_LINK_ACCESSED")
                .objectId(String.valueOf(note.getId()))
                .metadata("linkId=" + link.getId())
                .ipAddress(ipAddress)
                .outcome("SUCCESS")
                .build());

        log.info("Share link accessed. linkId={} noteId={} ip={}", link.getId(), note.getId(), ipAddress);
        return note;
    }

    // =========================================================================
    // Revoke
    // =========================================================================

    /**
     * Revokes a share link, preventing further access via that token.
     *
     * <p>Ownership verification: the share link's note must be owned by
     * {@code userId} to prevent one user revoking another's links.
     *
     * @param linkId  share link primary key
     * @param noteId  note the link belongs to (used for ownership resolution)
     * @param userId  authenticated user (must own the note)
     * @return {@code true} if revoked
     * @throws ServiceException NOT_FOUND or ACCESS_DENIED
     */
    public boolean revokeShareLink(Long linkId, Long noteId, Long userId) throws ServiceException {
        ShareLink link = requireShareLink(linkId);

        // Verify the link belongs to the claimed note.
        if (!link.getNoteId().equals(noteId)) {
            throw new ServiceException("NOT_FOUND", "Share link not found.");
        }

        // Verify caller owns the note.
        Note note = requireNote(noteId);
        requireOwnership(note, userId);

        if (link.getRevokedAt() != null) {
            // Idempotent — already revoked; treat as success.
            return true;
        }

        link.setRevokedAt(Instant.now());
        boolean revoked = shareLinkDao.revoke(linkId, link.getRevokedAt());

        if (revoked) {
            auditService.recordEvent(AuditEvent.builder()
                    .action("SHARE_LINK_REVOKED")
                    .subjectId(String.valueOf(userId))
                    .objectId(String.valueOf(noteId))
                    .metadata("linkId=" + linkId)
                    .outcome("SUCCESS")
                    .build());
            log.info("Share link revoked. linkId={} noteId={} userId={}", linkId, noteId, userId);
        }

        return revoked;
    }

    // =========================================================================
    // List active links
    // =========================================================================

    /**
     * Returns all active (non-revoked) share links for a note.
     *
     * <p>Ownership is verified before the query; users may only list their
     * own note's links.
     *
     * @param noteId note primary key
     * @param userId authenticated user (must own the note)
     * @return list of active {@link ShareLink} records
     * @throws ServiceException NOT_FOUND or ACCESS_DENIED
     */
    public List<ShareLink> getActiveLinksForNote(Long noteId, Long userId) throws ServiceException {
        Note note = requireNote(noteId);
        requireOwnership(note, userId);
        return shareLinkDao.findActiveByNoteId(noteId);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Note requireNote(Long noteId) throws ServiceException {
        if (noteId == null) {
            throw new ServiceException("VALIDATION", "Note ID must not be null.");
        }
        Note note = noteDao.findById(noteId);
        if (note == null) {
            throw new ServiceException("NOT_FOUND", "Note not found.");
        }
        return note;
    }

    private ShareLink requireShareLink(Long linkId) throws ServiceException {
        if (linkId == null) {
            throw new ServiceException("VALIDATION", "Link ID must not be null.");
        }
        ShareLink link = shareLinkDao.findById(linkId);
        if (link == null) {
            throw new ServiceException("NOT_FOUND", "Share link not found.");
        }
        return link;
    }

    private void requireOwnership(Note note, Long userId) throws ServiceException {
        if (userId == null || !userId.equals(note.getUserId())) {
            log.warn("ACCESS_DENIED: share link operation by non-owner. noteId={} userId={}",
                    note.getId(), userId);
            throw new ServiceException("ACCESS_DENIED",
                    "You do not have permission to manage share links for this note.");
        }
    }
}
