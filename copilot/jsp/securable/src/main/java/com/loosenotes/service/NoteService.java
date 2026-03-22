package com.loosenotes.service;

import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.model.ShareLink;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/** Business logic contract for note management. */
public interface NoteService {

    Note createNote(long userId, String title, String content, Note.Visibility visibility);

    Optional<Note> findById(long noteId);

    List<Note> findByUser(long userId);

    /** Verifies ownership before update; throws ServiceException if unauthorized. */
    void updateNote(long noteId, long requestingUserId,
                    String title, String content, Note.Visibility visibility);

    /** Verifies ownership or admin role before deletion. */
    void deleteNote(long noteId, long requestingUserId, boolean isAdmin);

    List<Note> search(String keyword, long requestingUserId);

    List<Note> findTopRated(int limit);

    // Attachments
    Attachment addAttachment(long noteId, long requestingUserId,
                             String originalFilename, InputStream data, long fileSize);
    Optional<Attachment> findAttachment(long attachmentId);
    void deleteAttachment(long attachmentId, long requestingUserId, boolean isAdmin);
    List<Attachment> getAttachments(long noteId);

    // Share links
    ShareLink generateShareLink(long noteId, long requestingUserId);
    void revokeShareLink(long noteId, long requestingUserId);
    Optional<ShareLink> findShareLink(long noteId);
    Optional<Note> findNoteByShareToken(String token);

    // Ratings
    Rating addOrUpdateRating(long noteId, long userId, int stars, String comment);
    List<Rating> getRatings(long noteId);
    double getAverageRating(long noteId);
    int getRatingCount(long noteId);
    Optional<Rating> getUserRating(long noteId, long userId);

    // Admin
    void reassignNote(long noteId, long newOwnerId, long adminId);

    int countAll();
}
