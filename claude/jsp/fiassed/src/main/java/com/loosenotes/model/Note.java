package com.loosenotes.model;

import java.time.LocalDateTime;

/**
 * Note domain model.
 * Visibility is a typed enum to prevent injection of invalid values (F-09, Integrity).
 * UserId is server-owned state — never accepted from client (F-04, Derived Integrity).
 */
public class Note {

    public enum Visibility {
        PUBLIC, PRIVATE
    }

    private Long id;
    private Long userId;
    private String username;  // denormalized for display
    private String title;
    private String content;
    private Visibility visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Rating summary (populated by join queries)
    private Double averageRating;
    private Integer ratingCount;

    public Note() {
        this.visibility = Visibility.PRIVATE;  // Default private (F-04)
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }

    public boolean isPublic() { return Visibility.PUBLIC.equals(visibility); }
}
