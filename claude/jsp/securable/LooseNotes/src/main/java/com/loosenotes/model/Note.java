package com.loosenotes.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a user note.
 * SSEM: Integrity - ownership tracked via userId.
 * SSEM: Resilience - defensive collection initialization.
 */
public class Note {

    private long id;
    private long userId;
    private String ownerUsername;
    private String title;
    private String content;
    private boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Attachment> attachments;
    private List<Rating> ratings;
    private double averageRating;
    private int ratingCount;

    public Note() {
        this.isPublic = false;
        this.attachments = new ArrayList<>();
        this.ratings = new ArrayList<>();
    }

    // ---- Getters ----

    public long getId() { return id; }

    public long getUserId() { return userId; }

    public String getOwnerUsername() { return ownerUsername; }

    public String getTitle() { return title; }

    public String getContent() { return content; }

    public boolean isPublic() { return isPublic; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** Returns an unmodifiable view to prevent external mutation. */
    public List<Attachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    /** Returns an unmodifiable view to prevent external mutation. */
    public List<Rating> getRatings() {
        return Collections.unmodifiableList(ratings);
    }

    public double getAverageRating() { return averageRating; }

    public int getRatingCount() { return ratingCount; }

    /** Returns content truncated to given length for display excerpts. */
    public String getExcerpt(int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    // ---- Setters ----

    public void setId(long id) { this.id = id; }

    public void setUserId(long userId) { this.userId = userId; }

    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public void setTitle(String title) { this.title = title; }

    public void setContent(String content) { this.content = content; }

    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = (attachments != null) ? new ArrayList<>(attachments) : new ArrayList<>();
    }

    public void setRatings(List<Rating> ratings) {
        this.ratings = (ratings != null) ? new ArrayList<>(ratings) : new ArrayList<>();
    }

    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
}
