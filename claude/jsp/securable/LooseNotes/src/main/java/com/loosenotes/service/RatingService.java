package com.loosenotes.service;

import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.RatingDao;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.util.InputSanitizer;
import com.loosenotes.util.ValidationUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for note ratings.
 * SSEM: Integrity - rating value constraint 1-5, comment sanitized.
 */
public class RatingService {

    private final RatingDao ratingDao;
    private final NoteDao noteDao;

    public RatingService(RatingDao ratingDao, NoteDao noteDao) {
        this.ratingDao = ratingDao;
        this.noteDao = noteDao;
    }

    /**
     * Submits or updates a rating for a note.
     * Trust boundary: validates and sanitizes inputs.
     *
     * @param noteId    note to rate
     * @param userId    rater's user ID
     * @param value     rating value 1-5
     * @param rawComment optional comment (may be null or blank)
     */
    public void submitRating(long noteId, long userId, int value,
                              String rawComment) throws SQLException {
        if (!ValidationUtil.isValidRatingValue(value)) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        String comment = InputSanitizer.sanitizeMultiLine(rawComment);
        if (!ValidationUtil.isValidRatingComment(comment)) {
            throw new IllegalArgumentException("Comment exceeds maximum length of 1000 characters");
        }

        verifyNoteAccessible(noteId, userId);

        Rating rating = new Rating();
        rating.setNoteId(noteId);
        rating.setUserId(userId);
        rating.setValue(value);
        rating.setComment(comment != null && comment.isBlank() ? null : comment);
        ratingDao.upsert(rating);
    }

    /** Returns all ratings for a note. */
    public List<Rating> getRatings(long noteId) throws SQLException {
        return ratingDao.findByNoteId(noteId);
    }

    /** Returns the current user's rating for a note, if any. */
    public Optional<Rating> getUserRating(long noteId, long userId) throws SQLException {
        return ratingDao.findByNoteAndUser(noteId, userId);
    }

    /**
     * Verifies the note exists and is accessible (public or owned).
     * Users can only rate notes they can view.
     */
    private void verifyNoteAccessible(long noteId, long userId) throws SQLException {
        Optional<Note> note = noteDao.findById(noteId);
        if (note.isEmpty()) {
            throw new IllegalArgumentException("Note not found");
        }
        boolean ownedByUser = note.get().getUserId() == userId;
        if (!ownedByUser && !note.get().isPublic()) {
            throw new SecurityException("Cannot rate a private note owned by another user");
        }
    }
}
