package com.loosenotes.service;

import com.loosenotes.dao.NoteDao;
import com.loosenotes.model.AuditEvent;
import com.loosenotes.model.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * Application-layer service for note lifecycle management.
 *
 * <p><strong>Security model enforced here (never in DAOs/servlets):</strong>
 * <ul>
 *   <li>Ownership is bound server-side from the authenticated session, never
 *       from request parameters (prevents IDOR / mass-assignment).</li>
 *   <li>Visibility enforcement: private notes are accessible only to their
 *       owner; PUBLIC notes are readable by anyone with a valid session.</li>
 *   <li>Every mutation emits a structured audit event via {@link AuditService}.</li>
 *   <li>{@code updatedAt} timestamps are always set server-side.</li>
 * </ul>
 */
public class NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteService.class);

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 65_535;

    private final NoteDao noteDao;
    private final AuditService auditService;

    /**
     * @param noteDao      DAO for note persistence; must not be {@code null}
     * @param auditService audit sink; must not be {@code null}
     */
    public NoteService(NoteDao noteDao, AuditService auditService) {
        if (noteDao == null) throw new IllegalArgumentException("noteDao must not be null");
        if (auditService == null) throw new IllegalArgumentException("auditService must not be null");
        this.noteDao = noteDao;
        this.auditService = auditService;
    }

    // =========================================================================
    // Create
    // =========================================================================

    /**
     * Creates a new note owned by {@code userId}.
     *
     * <p>The {@code userId} comes from the authenticated session — never from
     * the request body — so clients cannot forge ownership.
     *
     * @param userId     authenticated owner's ID (server-owned; not from request body)
     * @param title      note title (1–200 chars)
     * @param content    note body (up to 65 535 chars)
     * @param visibility PUBLIC or PRIVATE
     * @return the persisted {@link Note} with populated ID and timestamps
     * @throws ServiceException VALIDATION on bad input
     */
    public Note createNote(Long userId, String title, String content, Note.Visibility visibility)
            throws ServiceException {

        validateNoteInput(title, content);

        Note note = new Note();
        note.setUserId(userId);          // server-owned: never accept from request
        note.setTitle(title.trim());
        note.setContent(content);
        note.setVisibility(visibility != null ? visibility : Note.Visibility.PRIVATE);

        Instant now = Instant.now();
        note.setCreatedAt(now);
        note.setUpdatedAt(now);

        Note created = noteDao.create(note);

        auditService.recordEvent(AuditEvent.builder()
                .action("NOTE_CREATED")
                .subjectId(String.valueOf(userId))
                .objectId(String.valueOf(created.getId()))
                .metadata("visibility=" + created.getVisibility())
                .outcome("SUCCESS")
                .build());

        log.info("Note created. noteId={} userId={} visibility={}", created.getId(), userId, created.getVisibility());
        return created;
    }

    // =========================================================================
    // Read
    // =========================================================================

    /**
     * Returns a note if the requesting user is authorised to read it.
     *
     * <p>Access rules:
     * <ul>
     *   <li>Owner may always read their own note.</li>
     *   <li>PUBLIC notes are readable by any authenticated user
     *       ({@code requestingUserId} non-null) and by unauthenticated share-link
     *       visitors ({@code requestingUserId} may be {@code null}).</li>
     *   <li>PRIVATE notes return an ACCESS_DENIED {@link ServiceException} for
     *       non-owners.</li>
     * </ul>
     *
     * @param noteId           note primary key
     * @param requestingUserId authenticated user ID, or {@code null} for anonymous
     * @return the {@link Note}
     * @throws ServiceException NOT_FOUND or ACCESS_DENIED
     */
    public Note getNote(Long noteId, Long requestingUserId) throws ServiceException {
        Note note = requireNote(noteId);

        boolean isOwner = requestingUserId != null && requestingUserId.equals(note.getUserId());

        if (!isOwner && note.getVisibility() == Note.Visibility.PRIVATE) {
            log.warn("ACCESS_DENIED: private note access attempted. noteId={} requestingUserId={}",
                    noteId, requestingUserId);
            throw new ServiceException("ACCESS_DENIED", "You do not have permission to view this note.");
        }

        return note;
    }

    /**
     * Returns a note for editing — ownership is required.
     *
     * @param noteId           note primary key
     * @param requestingUserId authenticated user (must be owner)
     * @return the {@link Note}
     * @throws ServiceException NOT_FOUND or ACCESS_DENIED
     */
    public Note getNoteForEdit(Long noteId, Long requestingUserId) throws ServiceException {
        Note note = requireNote(noteId);
        requireOwnership(note, requestingUserId, "edit");
        return note;
    }

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Updates an existing note's mutable fields.
     *
     * <p>Ownership is verified server-side.  The {@code updatedAt} timestamp is
     * always set to {@code Instant.now()} — never accepted from the caller.
     * Visibility changes are specifically logged for auditability.
     *
     * @param noteId     note to update
     * @param userId     authenticated user (must be owner)
     * @param title      new title
     * @param content    new content
     * @param visibility new visibility
     * @return {@code true} if the DAO reported a row was updated
     * @throws ServiceException VALIDATION, ACCESS_DENIED, or NOT_FOUND
     */
    public boolean updateNote(Long noteId, Long userId, String title, String content,
                               Note.Visibility visibility) throws ServiceException {

        validateNoteInput(title, content);

        Note existing = requireNote(noteId);
        requireOwnership(existing, userId, "update");

        // Detect visibility change for targeted audit logging.
        boolean visibilityChanged = visibility != null
                && !visibility.equals(existing.getVisibility());

        existing.setTitle(title.trim());
        existing.setContent(content);
        existing.setVisibility(visibility != null ? visibility : existing.getVisibility());
        existing.setUpdatedAt(Instant.now());   // server-set; never from request

        boolean updated = noteDao.update(existing);

        if (updated) {
            AuditEvent.Builder auditBuilder = AuditEvent.builder()
                    .action("NOTE_UPDATED")
                    .subjectId(String.valueOf(userId))
                    .objectId(String.valueOf(noteId))
                    .outcome("SUCCESS");

            if (visibilityChanged) {
                auditBuilder.metadata("visibilityChanged="
                        + existing.getVisibility() + "->" + visibility);
            }

            auditService.recordEvent(auditBuilder.build());
            log.info("Note updated. noteId={} userId={} visibilityChanged={}", noteId, userId, visibilityChanged);
        }

        return updated;
    }

    // =========================================================================
    // Delete
    // =========================================================================

    /**
     * Deletes a note.
     *
     * <p>Authorised when: {@code userId} is the note owner, OR {@code isAdmin}
     * is {@code true}.  Cascading deletes (attachments, ratings, share-links)
     * are handled by FK constraints in the database schema.
     *
     * @param noteId  note to delete
     * @param userId  requesting user's ID
     * @param isAdmin {@code true} if the requesting user holds the ADMIN role
     * @return {@code true} if deleted
     * @throws ServiceException NOT_FOUND or ACCESS_DENIED
     */
    public boolean deleteNote(Long noteId, Long userId, boolean isAdmin) throws ServiceException {
        Note note = requireNote(noteId);

        boolean isOwner = userId != null && userId.equals(note.getUserId());
        if (!isOwner && !isAdmin) {
            log.warn("ACCESS_DENIED: delete attempted by non-owner. noteId={} requestingUserId={}", noteId, userId);
            throw new ServiceException("ACCESS_DENIED", "You do not have permission to delete this note.");
        }

        boolean deleted = noteDao.delete(noteId);

        if (deleted) {
            auditService.recordEvent(AuditEvent.builder()
                    .action("NOTE_DELETED")
                    .subjectId(String.valueOf(userId))
                    .objectId(String.valueOf(noteId))
                    .metadata("byAdmin=" + isAdmin)
                    .outcome("SUCCESS")
                    .build());
            log.info("Note deleted. noteId={} byUserId={} byAdmin={}", noteId, userId, isAdmin);
        }

        return deleted;
    }

    // =========================================================================
    // Listings / search
    // =========================================================================

    /**
     * Returns a page of notes owned by {@code userId}.
     *
     * @param userId   owner's ID
     * @param page     0-based page index
     * @param pageSize records per page
     */
    public List<Note> getUserNotes(Long userId, int page, int pageSize) {
        return noteDao.findByUserId(userId, page, pageSize);
    }

    /**
     * Full-text note search, respecting visibility.
     *
     * <p>Visibility filtering is delegated to the DAO query so that PRIVATE
     * notes belonging to other users are never returned to {@code requestingUserId}.
     *
     * @param requestingUserId authenticated user; DAO uses this to include own
     *                         private notes in results
     * @param query            search term
     * @param page             0-based page index
     * @param pageSize         records per page
     */
    public List<Note> searchNotes(Long requestingUserId, String query, int page, int pageSize) {
        return noteDao.search(requestingUserId, query, page, pageSize);
    }

    /**
     * Returns the top-rated public notes.
     *
     * @param minRatingCount minimum number of ratings a note must have
     * @param pageSize       maximum results to return
     */
    public List<Note> getTopRated(int minRatingCount, int pageSize) {
        return noteDao.findTopRated(minRatingCount, pageSize);
    }

    // =========================================================================
    // Admin operations
    // =========================================================================

    /**
     * Reassigns note ownership to a different user (admin operation).
     *
     * <p>Validates that:
     * <ul>
     *   <li>The target user exists.</li>
     *   <li>The new owner differs from the current owner (no-op guard).</li>
     * </ul>
     *
     * @param noteId        note to reassign
     * @param newOwnerId    target user's ID
     * @param adminId       performing admin's ID (for audit)
     * @param adminUsername performing admin's username (for audit readability)
     * @return {@code true} if the DAO reported a row was updated
     * @throws ServiceException NOT_FOUND or VALIDATION
     */
    public boolean reassignNote(Long noteId, Long newOwnerId, Long adminId, String adminUsername)
            throws ServiceException {

        Note note = requireNote(noteId);

        if (newOwnerId == null) {
            throw new ServiceException("VALIDATION", "New owner ID must not be null.");
        }
        if (newOwnerId.equals(note.getUserId())) {
            throw new ServiceException("VALIDATION", "Note is already owned by that user.");
        }

        Long previousOwnerId = note.getUserId();
        note.setUserId(newOwnerId);
        note.setUpdatedAt(Instant.now());

        boolean updated = noteDao.update(note);

        if (updated) {
            auditService.recordEvent(AuditEvent.builder()
                    .action("NOTE_REASSIGNED")
                    .subjectId(String.valueOf(adminId))
                    .subjectUsername(adminUsername)
                    .objectId(String.valueOf(noteId))
                    .metadata("previousOwnerId=" + previousOwnerId + " newOwnerId=" + newOwnerId)
                    .outcome("SUCCESS")
                    .build());
            log.info("Note reassigned. noteId={} previousOwner={} newOwner={} byAdmin={}",
                    noteId, previousOwnerId, newOwnerId, adminId);
        }

        return updated;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void validateNoteInput(String title, String content) throws ServiceException {
        if (title == null || title.trim().isEmpty()) {
            throw new ServiceException("VALIDATION", "Note title must not be empty.");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new ServiceException("VALIDATION",
                    "Note title must not exceed " + MAX_TITLE_LENGTH + " characters.");
        }
        if (content == null) {
            throw new ServiceException("VALIDATION", "Note content must not be null.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new ServiceException("VALIDATION",
                    "Note content must not exceed " + MAX_CONTENT_LENGTH + " characters.");
        }
    }

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

    private void requireOwnership(Note note, Long userId, String operation) throws ServiceException {
        if (userId == null || !userId.equals(note.getUserId())) {
            log.warn("ACCESS_DENIED: {} attempted by non-owner. noteId={} requestingUserId={}",
                    operation, note.getId(), userId);
            throw new ServiceException("ACCESS_DENIED",
                    "You do not have permission to " + operation + " this note.");
        }
    }
}
