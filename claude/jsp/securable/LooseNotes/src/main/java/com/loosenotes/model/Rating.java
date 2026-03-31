package com.loosenotes.model;

import java.time.Instant;

/**
 * Represents a user's rating (1-5 stars) on a note.
 *
 * SSEM notes:
 * - Integrity: stars range constraint validated in service layer AND DB CHECK constraint.
 */
public class Rating {

    private long id;
    private long noteId;
    private long userId;
    /** Denormalized for display. */
    private String raterUsername;
    /** 1–5 inclusive; validated at service layer. */
    private int stars;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;

    public Rating() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getNoteId() { return noteId; }
    public void setNoteId(long noteId) { this.noteId = noteId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getRaterUsername() { return raterUsername; }
    public void setRaterUsername(String raterUsername) { this.raterUsername = raterUsername; }

    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Rating{id=" + id + ", noteId=" + noteId + ", userId=" + userId + ", stars=" + stars + '}';
    }
}
