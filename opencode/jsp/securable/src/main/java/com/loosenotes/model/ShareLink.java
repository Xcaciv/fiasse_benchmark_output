package com.loosenotes.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class ShareLink implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Long noteId;
    private String token;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    
    private Note note;
    
    public ShareLink() {
        this.token = UUID.randomUUID().toString();
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }
    
    public ShareLink(Long noteId) {
        this();
        this.noteId = noteId;
    }
    
    public ShareLink(Long noteId, LocalDateTime expiresAt) {
        this(noteId);
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
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
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
    
    public Note getNote() {
        return note;
    }
    
    public void setNote(Note note) {
        this.note = note;
    }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return active && !isExpired();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShareLink shareLink = (ShareLink) o;
        return Objects.equals(id, shareLink.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
