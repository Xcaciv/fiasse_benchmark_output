package com.loosenotes.model;

public class Rating {
    private final long id;
    private final long noteId;
    private final long userId;
    private final String username;
    private final int ratingValue;
    private final String comment;
    private final String createdAt;
    private final String updatedAt;

    public Rating(long id, long noteId, long userId, String username, int ratingValue, String comment, String createdAt, String updatedAt) {
        this.id = id;
        this.noteId = noteId;
        this.userId = userId;
        this.username = username;
        this.ratingValue = ratingValue;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() { return id; }
    public long getNoteId() { return noteId; }
    public long getUserId() { return userId; }
    public String getUsername() { return username; }
    public int getRatingValue() { return ratingValue; }
    public String getComment() { return comment; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
