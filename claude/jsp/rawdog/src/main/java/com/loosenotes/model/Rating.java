package com.loosenotes.model;

import java.sql.Timestamp;

public class Rating {
    private int id;
    private int noteId;
    private int userId;
    private int rating;
    private String comment;
    private Timestamp createdAt;
    // Extra field for display
    private String raterUsername;

    public Rating() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNoteId() { return noteId; }
    public void setNoteId(int noteId) { this.noteId = noteId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getRaterUsername() { return raterUsername; }
    public void setRaterUsername(String raterUsername) { this.raterUsername = raterUsername; }
}
