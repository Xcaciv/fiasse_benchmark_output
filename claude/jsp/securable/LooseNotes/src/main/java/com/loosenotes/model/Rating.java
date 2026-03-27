package com.loosenotes.model;

import java.time.LocalDateTime;

/**
 * Represents a user rating on a note.
 * SSEM: Integrity - value constrained 1-5 at DB and validation level.
 */
public class Rating {

    private long id;
    private long noteId;
    private long userId;
    private String raterUsername;
    private int value;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Rating() {}

    // ---- Getters ----

    public long getId() { return id; }

    public long getNoteId() { return noteId; }

    public long getUserId() { return userId; }

    public String getRaterUsername() { return raterUsername; }

    public int getValue() { return value; }

    public String getComment() { return comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ---- Setters ----

    public void setId(long id) { this.id = id; }

    public void setNoteId(long noteId) { this.noteId = noteId; }

    public void setUserId(long userId) { this.userId = userId; }

    public void setRaterUsername(String raterUsername) { this.raterUsername = raterUsername; }

    public void setValue(int value) { this.value = value; }

    public void setComment(String comment) { this.comment = comment; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
