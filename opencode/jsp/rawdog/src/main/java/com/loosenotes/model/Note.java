package com.loosenotes.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Note {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String shareToken;
    private boolean shareEnabled;
    
    private User owner;
    private List<Attachment> attachments;
    private List<Rating> ratings;
    private double averageRating;
    private int ratingCount;

    public Note() {
        this.isPublic = false;
        this.shareEnabled = false;
        this.createdAt = LocalDateTime.now();
        this.attachments = new ArrayList<>();
        this.ratings = new ArrayList<>();
    }

    public Note(Long userId, String title, String content) {
        this();
        this.userId = userId;
        this.title = title;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
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

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }

    public boolean isShareEnabled() {
        return shareEnabled;
    }

    public void setShareEnabled(boolean shareEnabled) {
        this.shareEnabled = shareEnabled;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public List<Rating> getRatings() {
        return ratings;
    }

    public void setRatings(List<Rating> ratings) {
        this.ratings = ratings;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    public String getExcerpt() {
        if (content == null || content.length() <= 200) {
            return content;
        }
        return content.substring(0, 200) + "...";
    }

    public void addAttachment(Attachment attachment) {
        this.attachments.add(attachment);
    }

    public void addRating(Rating rating) {
        this.ratings.add(rating);
    }
}
