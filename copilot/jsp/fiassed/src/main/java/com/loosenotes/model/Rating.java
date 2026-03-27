package com.loosenotes.model;

import java.time.LocalDateTime;

public class Rating {
    private Long id;
    private Long noteId;
    private Long userId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String raterUsername;

    public Rating() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getNoteId() { return noteId; }
    public void setNoteId(Long noteId) { this.noteId = noteId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getRaterUsername() { return raterUsername; }
    public void setRaterUsername(String raterUsername) { this.raterUsername = raterUsername; }
}
