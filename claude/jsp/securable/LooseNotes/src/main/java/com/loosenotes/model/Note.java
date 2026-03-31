package com.loosenotes.model;

import java.time.Instant;

/**
 * Represents a note created by a user.
 *
 * SSEM notes:
 * - Integrity: visibility is constrained via the Visibility enum, not a raw string.
 * - Analyzability: simple data-holder, no business logic.
 */
public class Note {

    /** Allowed visibility states – prevents invalid string values. */
    public enum Visibility {
        PRIVATE,
        PUBLIC
    }

    private long id;
    private long userId;
    /** Denormalized for display purposes – populated by JOIN queries. */
    private String authorUsername;
    private String title;
    private String content;
    private Visibility visibility;
    private Instant createdAt;
    private Instant updatedAt;
    /** Computed field – average rating from the ratings table. */
    private double averageRating;
    private int ratingCount;

    public Note() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }

    public boolean isPublic() { return Visibility.PUBLIC.equals(visibility); }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

    /** Returns the first 200 characters of content for search result excerpts. */
    public String getExcerpt() {
        if (content == null || content.isEmpty()) return "";
        return content.length() <= 200 ? content : content.substring(0, 200) + "…";
    }

    @Override
    public String toString() {
        return "Note{id=" + id + ", userId=" + userId + ", title='" + title + "', visibility=" + visibility + '}';
    }
}
