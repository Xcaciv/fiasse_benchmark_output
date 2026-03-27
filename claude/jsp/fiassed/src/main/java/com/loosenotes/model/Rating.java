package com.loosenotes.model;

import java.time.LocalDateTime;

/**
 * Rating domain model.
 * ratingValue constrained 1-5 by DB CHECK constraint and service validation (F-10, Integrity).
 */
public class Rating {

    private Long id;
    private Long noteId;
    private Long userId;
    private String username;   // denormalized for display
    private int ratingValue;   // 1-5 inclusive
    private String comment;    // max 1000 chars
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNoteId() { return noteId; }
    public void setNoteId(Long noteId) { this.noteId = noteId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getRatingValue() { return ratingValue; }
    public void setRatingValue(int ratingValue) { this.ratingValue = ratingValue; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
