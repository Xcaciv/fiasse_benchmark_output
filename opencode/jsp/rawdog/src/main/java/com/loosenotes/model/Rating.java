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
    
    private User rater;

    public Rating() {
        this.createdAt = LocalDateTime.now();
    }

    public Rating(Long noteId, Long userId, int rating, String comment) {
        this();
        this.noteId = noteId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getRater() {
        return rater;
    }

    public void setRater(User rater) {
        this.rater = rater;
    }

    public String getStarDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (i <= rating) {
                sb.append("\u2605");
            } else {
                sb.append("\u2606");
            }
        }
        return sb.toString();
    }
}
