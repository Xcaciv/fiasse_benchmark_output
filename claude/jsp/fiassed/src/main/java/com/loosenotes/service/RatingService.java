package com.loosenotes.service;

import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.RatingDao;
import com.loosenotes.model.AuditEvent;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * Application-layer service for note ratings (F-10, F-11).
 *
 * <p><strong>Integrity invariants enforced here:</strong>
 * <ul>
 *   <li>One rating per user per note — duplicates are rejected with a
 *       DUPLICATE {@link ServiceException}.</li>
 *   <li>Rating value is constrained to [1, 5] by validation before any
 *       persistence call.</li>
 *   <li>Ownership of an existing rating is verified before update/delete.</li>
 *   <li>Full rating lists (with commenter identities) are accessible only to
 *       the note owner or administrators, preventing scraping of user
 *       associations by anonymous parties.</li>
 * </ul>
 */
public class RatingService {

    private static final Logger log = LoggerFactory.getLogger(RatingService.class);

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MAX_COMMENT_LENGTH = 1000;

    private final RatingDao ratingDao;
    private final NoteDao noteDao;
    private final AuditService auditService;

    /**
     * @param ratingDao    DAO for rating persistence; must not be {@code null}
     * @param noteDao      DAO for note lookups (ownership check); must not be {@code null}
     * @param auditService audit sink; must not be {@code null}
     */
    public RatingService(RatingDao ratingDao, NoteDao noteDao, AuditService auditService) {
        if (ratingDao == null) throw new IllegalArgumentException("ratingDao must not be null");
        if (noteDao == null) throw new IllegalArgumentException("noteDao must not be null");
        if (auditService == null) throw new IllegalArgumentException("auditService must not be null");
        this.ratingDao = ratingDao;
        this.noteDao = noteDao;
        this.auditService = auditService;
    }

    // =========================================================================
    // Submit (create)
    // =========================================================================

    /**
     * Submits a new rating for a note.
     *
     * <p>One-rating-per-user-per-note is enforced; attempting a second rating
     * throws a DUPLICATE {@link ServiceException}.
     *
     * @param noteId      note being rated
     * @param userId      authenticated rater's ID
     * @param ratingValue integer in [1, 5]
     * @param comment     optional comment (max 1000 chars); {@code null} is accepted
     * @return the persisted {@link Rating}
     * @throws ServiceException VALIDATION on bad input, DUPLICATE on second rating,
     *                          NOT_FOUND if note doesn't exist
     */
    public Rating submitRating(Long noteId, Long userId, int ratingValue, String comment)
            throws ServiceException {

        validateRatingValue(ratingValue);
        validateComment(comment);
        requireNoteExists(noteId);

        // One-rating-per-user-per-note check.
        Rating existing = ratingDao.findByNoteAndUser(noteId, userId);
        if (existing != null) {
            throw new ServiceException("DUPLICATE",
                    "You have already rated this note. Use the update endpoint to change your rating.");
        }

        Rating rating = new Rating();
        rating.setNoteId(noteId);
        rating.setUserId(userId);
        rating.setRatingValue(ratingValue);
        rating.setComment(comment != null ? comment.trim() : null);

        Instant now = Instant.now();
        rating.setCreatedAt(now);
        rating.setUpdatedAt(now);

        Rating created = ratingDao.create(rating);

        auditService.recordEvent(AuditEvent.builder()
                .action("RATING_SUBMITTED")
                .subjectId(String.valueOf(userId))
                .objectId(String.valueOf(noteId))
                .metadata("ratingId=" + created.getId() + " value=" + ratingValue)
                .outcome("SUCCESS")
                .build());

        log.info("Rating submitted. ratingId={} noteId={} userId={} value={}",
                created.getId(), noteId, userId, ratingValue);
        return created;
    }

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Updates an existing rating.
     *
     * <p>Ownership is verified: {@code userId} must match the rating's owner.
     *
     * @param ratingId    rating to update
     * @param userId      authenticated user (must own the rating)
     * @param ratingValue new integer value in [1, 5]
     * @param comment     new comment (max 1000 chars); {@code null} clears comment
     * @return the updated {@link Rating}
     * @throws ServiceException VALIDATION, ACCESS_DENIED, or NOT_FOUND
     */
    public Rating updateRating(Long ratingId, Long userId, int ratingValue, String comment)
            throws ServiceException {

        validateRatingValue(ratingValue);
        validateComment(comment);

        Rating rating = requireRating(ratingId);
        requireRatingOwnership(rating, userId);

        rating.setRatingValue(ratingValue);
        rating.setComment(comment != null ? comment.trim() : null);
        rating.setUpdatedAt(Instant.now());

        ratingDao.update(rating);

        auditService.recordEvent(AuditEvent.builder()
                .action("RATING_UPDATED")
                .subjectId(String.valueOf(userId))
                .objectId(String.valueOf(rating.getNoteId()))
                .metadata("ratingId=" + ratingId + " newValue=" + ratingValue)
                .outcome("SUCCESS")
                .build());

        log.info("Rating updated. ratingId={} userId={} newValue={}", ratingId, userId, ratingValue);
        return rating;
    }

    // =========================================================================
    // Delete
    // =========================================================================

    /**
     * Deletes a rating.
     *
     * <p>Ownership is verified; only the rating's author may delete it.
     *
     * @param ratingId rating to delete
     * @param userId   authenticated user (must own the rating)
     * @return {@code true} if deleted
     * @throws ServiceException ACCESS_DENIED or NOT_FOUND
     */
    public boolean deleteRating(Long ratingId, Long userId) throws ServiceException {
        Rating rating = requireRating(ratingId);
        requireRatingOwnership(rating, userId);

        boolean deleted = ratingDao.delete(ratingId);

        if (deleted) {
            auditService.recordEvent(AuditEvent.builder()
                    .action("RATING_DELETED")
                    .subjectId(String.valueOf(userId))
                    .objectId(String.valueOf(rating.getNoteId()))
                    .metadata("ratingId=" + ratingId)
                    .outcome("SUCCESS")
                    .build());
            log.info("Rating deleted. ratingId={} userId={}", ratingId, userId);
        }

        return deleted;
    }

    // =========================================================================
    // Query
    // =========================================================================

    /**
     * Returns a page of ratings for a note.
     *
     * <p><strong>Access control</strong>: the full list (including rater
     * identities) is available only to the note owner or administrators,
     * preventing correlation of user accounts via public rating history.
     * Passing {@code null} for {@code requestingUserId} always triggers the
     * ACCESS_DENIED path.
     *
     * @param noteId           note whose ratings are requested
     * @param requestingUserId authenticated user (must be note owner or admin)
     * @param page             0-based page index
     * @param pageSize         records per page
     * @return paginated list of {@link Rating}
     * @throws ServiceException NOT_FOUND or ACCESS_DENIED
     */
    public List<Rating> getRatingsForNote(Long noteId, Long requestingUserId, int page, int pageSize)
            throws ServiceException {

        Note note = requireNoteExists(noteId);

        boolean isOwner = requestingUserId != null && requestingUserId.equals(note.getUserId());
        if (!isOwner) {
            log.warn("ACCESS_DENIED: rating list requested by non-owner. noteId={} requestingUserId={}",
                    noteId, requestingUserId);
            throw new ServiceException("ACCESS_DENIED",
                    "Only the note owner may view the full rating list.");
        }

        return ratingDao.findByNoteId(noteId, page, pageSize);
    }

    /**
     * Returns the total number of ratings for a note.
     *
     * @param noteId note primary key
     */
    public int countRatingsForNote(Long noteId) {
        return ratingDao.countByNoteId(noteId);
    }

    /**
     * Returns the rating made by a specific user on a specific note, or
     * {@code null} if none exists.
     *
     * @param noteId note primary key
     * @param userId rater's user ID
     */
    public Rating findByNoteAndUser(Long noteId, Long userId) {
        return ratingDao.findByNoteAndUser(noteId, userId);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void validateRatingValue(int value) throws ServiceException {
        if (value < MIN_RATING || value > MAX_RATING) {
            throw new ServiceException("VALIDATION",
                    "Rating value must be between " + MIN_RATING + " and " + MAX_RATING + ".");
        }
    }

    private void validateComment(String comment) throws ServiceException {
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw new ServiceException("VALIDATION",
                    "Comment must not exceed " + MAX_COMMENT_LENGTH + " characters.");
        }
    }

    private Note requireNoteExists(Long noteId) throws ServiceException {
        if (noteId == null) {
            throw new ServiceException("VALIDATION", "Note ID must not be null.");
        }
        Note note = noteDao.findById(noteId);
        if (note == null) {
            throw new ServiceException("NOT_FOUND", "Note not found.");
        }
        return note;
    }

    private Rating requireRating(Long ratingId) throws ServiceException {
        if (ratingId == null) {
            throw new ServiceException("VALIDATION", "Rating ID must not be null.");
        }
        Rating rating = ratingDao.findById(ratingId);
        if (rating == null) {
            throw new ServiceException("NOT_FOUND", "Rating not found.");
        }
        return rating;
    }

    private void requireRatingOwnership(Rating rating, Long userId) throws ServiceException {
        if (userId == null || !userId.equals(rating.getUserId())) {
            log.warn("ACCESS_DENIED: rating ownership check failed. ratingId={} requestingUserId={}",
                    rating.getId(), userId);
            throw new ServiceException("ACCESS_DENIED",
                    "You do not have permission to modify this rating.");
        }
    }
}
