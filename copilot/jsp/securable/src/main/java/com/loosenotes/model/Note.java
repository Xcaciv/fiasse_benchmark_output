package com.loosenotes.model;

public class Note {
    private final long id;
    private final long ownerId;
    private final String ownerUsername;
    private final String title;
    private final String content;
    private final boolean publicNote;
    private final String createdAt;
    private final String updatedAt;
    private final double averageRating;
    private final int ratingCount;
    private final String excerpt;

    public Note(long id, long ownerId, String ownerUsername, String title, String content, boolean publicNote,
                String createdAt, String updatedAt, double averageRating, int ratingCount, String excerpt) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerUsername = ownerUsername;
        this.title = title;
        this.content = content;
        this.publicNote = publicNote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
        this.excerpt = excerpt;
    }

    public long getId() { return id; }
    public long getOwnerId() { return ownerId; }
    public String getOwnerUsername() { return ownerUsername; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isPublicNote() { return publicNote; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public double getAverageRating() { return averageRating; }
    public int getRatingCount() { return ratingCount; }
    public String getExcerpt() { return excerpt; }
}
