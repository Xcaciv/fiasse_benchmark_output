package com.loosenotes.dao;

import com.loosenotes.model.Rating;
import java.util.List;
import java.util.Optional;

/** Data access contract for note ratings. */
public interface RatingDao {
    long insert(Rating rating);
    Optional<Rating> findById(long id);
    Optional<Rating> findByNoteAndUser(long noteId, long userId);
    List<Rating> findByNoteId(long noteId);
    boolean update(Rating rating);
    double getAverageForNote(long noteId);
    int countForNote(long noteId);
}
