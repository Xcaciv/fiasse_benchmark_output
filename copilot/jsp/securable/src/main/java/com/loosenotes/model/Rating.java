package com.loosenotes.model;

/**
 * A user's rating for a note (1–5 stars with optional comment).
 * UNIQUE constraint on (note_id, user_id) is enforced at the DB level.
 */
public class Rating {

    private long id;
    private long noteId;
    private long userId;
    private int ratingValue;
    private String comment;
    private long createdAt;
    private long updatedAt;

    /** Transient display field joined from users table. */
    private String username;

    public Rating() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getNoteId() { return noteId; }
    public void setNoteId(long noteId) { this.noteId = noteId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public int getRatingValue() { return ratingValue; }
    public void setRatingValue(int ratingValue) { this.ratingValue = ratingValue; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
