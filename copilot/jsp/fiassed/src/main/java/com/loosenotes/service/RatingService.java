package com.loosenotes.service;

import com.loosenotes.dao.NoteDao;
import com.loosenotes.dao.RatingDao;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RatingService {
    private static final Logger logger = LoggerFactory.getLogger(RatingService.class);
    private final RatingDao ratingDao = new RatingDao();
    private final NoteDao noteDao = new NoteDao();

    public boolean rateNote(long noteId, long userId, int rating, String comment) {
        if (rating < 1 || rating > 5) return false;
        if (comment != null && comment.length() > 1000) return false;
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            Optional<Note> noteOpt = noteDao.findById(conn, noteId);
            if (noteOpt.isEmpty() || !noteOpt.get().isPublic()) return false;
            if (noteOpt.get().getUserId().equals(userId)) return false; // Cannot rate own note
            Rating r = new Rating();
            r.setNoteId(noteId);
            r.setUserId(userId);
            r.setRating(rating);
            r.setComment(comment);
            ratingDao.upsert(conn, r);
            return true;
        } catch (Exception e) {
            logger.error("Error rating note", e);
            return false;
        }
    }

    public List<Rating> findByNoteId(long noteId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return ratingDao.findByNoteId(conn, noteId);
        } catch (Exception e) {
            logger.error("Error getting ratings", e);
            return Collections.emptyList();
        }
    }

    public Optional<Rating> findUserRating(long noteId, long userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return ratingDao.findByNoteAndUser(conn, noteId, userId);
        } catch (Exception e) {
            logger.error("Error finding user rating", e);
            return Optional.empty();
        }
    }
}
