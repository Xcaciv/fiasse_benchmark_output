package com.loosenotes.model;

/**
 * Represents a user note. Private by default (isPublic = false).
 * All timestamps are epoch milliseconds.
 */
public class Note {

    private long id;
    private long userId;
    private String title;
    private String content;
    private boolean isPublic;
    private long createdAt;
    private long updatedAt;

    /** Transient display field — not stored in notes table. */
    private String ownerUsername;

    public Note() {
        this.isPublic = false;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }
}
