package com.loosenotes.dao;

import com.loosenotes.model.Note;
import java.util.List;
import java.util.Optional;

/** Data access contract for notes. Implementations use parameterized queries only. */
public interface NoteDao {
    long insert(Note note);
    Optional<Note> findById(long id);
    List<Note> findByUserId(long userId);
    List<Note> findPublicNotes();
    List<Note> searchNotes(String keyword, long requestingUserId);
    List<Note> findTopRated(int minRatings, int limit);
    boolean update(Note note);
    boolean delete(long id);
    boolean changeOwner(long noteId, long newOwnerId);
    int countAll();
}
