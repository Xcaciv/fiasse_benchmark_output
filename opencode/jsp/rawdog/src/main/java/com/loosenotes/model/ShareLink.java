package com.loosenotes.model;

import java.time.LocalDateTime;

public class ShareLink {
    private Long id;
    private Long noteId;
    private String shareToken;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public ShareLink() {
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    public ShareLink(Long noteId, String shareToken) {
        this();
        this.noteId = noteId;
        this.shareToken = shareToken;
    }

    public ShareLink(Long noteId, String shareToken, LocalDateTime expiresAt) {
        this(noteId, shareToken);
        this.expiresAt = expiresAt;
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

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
}
