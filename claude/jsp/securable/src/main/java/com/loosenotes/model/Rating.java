package com.loosenotes.model;

import java.time.LocalDateTime;

public class Rating {
    private long id;
    private long noteId;
    private long userId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;

    // Joined field
    private String raterUsername;

    public Rating() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getNoteId() { return noteId; }
    public void setNoteId(long noteId) { this.noteId = noteId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getRaterUsername() { return raterUsername; }
    public void setRaterUsername(String raterUsername) { this.raterUsername = raterUsername; }
}
