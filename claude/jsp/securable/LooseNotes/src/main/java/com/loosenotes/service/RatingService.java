package com.loosenotes.service;

import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.RatingDao;
import com.loosenotes.model.AuditLog.EventType;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for note ratings.
 *
 * SSEM notes:
 * - Integrity: stars range validated via ValidationUtil before persistence.
 * - Confidentiality: users cannot see other users' private notes (enforced via NoteDao).
 */
public class RatingService {

    private static final Logger log = LoggerFactory.getLogger(RatingService.class);

    private final RatingDao ratingDao;
    private final NoteDao noteDao;
    private final AuditService auditService;

    public RatingService(RatingDao ratingDao, NoteDao noteDao, AuditService auditService) {
        this.ratingDao    = ratingDao;
        this.noteDao      = noteDao;
        this.auditService = auditService;
    }

    /** Returns all ratings for a publicly visible note. */
    public List<Rating> getRatingsForNote(long noteId) throws ServiceException, SQLException {
        noteDao.findById(noteId).orElseThrow(() -> new ServiceException("Note not found"));
        return ratingDao.findByNoteId(noteId);
    }

    /**
     * Submits or updates a rating. A user may have at most one rating per note.
     * Users cannot rate their own note.
     */
    public void rate(long noteId, long userId, int stars, String comment, String ipAddress)
            throws ServiceException, SQLException {

        if (!ValidationUtil.isValidRating(stars)) {
            throw new ServiceException("Rating must be between 1 and 5");
        }

        Note note = noteDao.findById(noteId)
                .orElseThrow(() -> new ServiceException("Note not found"));

        if (note.getUserId() == userId) {
            throw new ServiceException("You cannot rate your own note");
        }

        Optional<Rating> existing = ratingDao.findByNoteAndUser(noteId, userId);
        if (existing.isPresent()) {
            // Update existing rating
            Rating r = existing.get();
            r.setStars(stars);
            r.setComment(comment);
            ratingDao.update(r);
            auditService.record(userId, EventType.RATING,
                    "rating_updated noteId=" + noteId, ipAddress);
        } else {
            // Insert new rating
            Rating r = new Rating();
            r.setNoteId(noteId);
            r.setUserId(userId);
            r.setStars(stars);
            r.setComment(comment);
            ratingDao.insert(r);
            auditService.record(userId, EventType.RATING,
                    "rating_created noteId=" + noteId + " stars=" + stars, ipAddress);
        }
    }

    /** Returns whether the given user has already rated this note. */
    public Optional<Rating> getExistingRating(long noteId, long userId) throws SQLException {
        return ratingDao.findByNoteAndUser(noteId, userId);
    }
}
