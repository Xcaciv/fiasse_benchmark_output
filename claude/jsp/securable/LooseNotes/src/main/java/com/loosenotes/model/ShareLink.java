package com.loosenotes.model;

import java.time.LocalDateTime;

/**
 * Represents a share link for a note.
 * SSEM: Authenticity - token is cryptographically random.
 * SSEM: Confidentiality - token is opaque, not derivable from note ID.
 */
public class ShareLink {

    private long id;
    private long noteId;
    /** Cryptographically random URL-safe token. Never log this value. */
    private String token;
    private LocalDateTime createdAt;
    private boolean active;

    public ShareLink() {
        this.active = true;
    }

    // ---- Getters ----

    public long getId() { return id; }

    public long getNoteId() { return noteId; }

    /** Returns the share token. Handle with care - treat as a credential. */
    public String getToken() { return token; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isActive() { return active; }

    // ---- Setters ----

    public void setId(long id) { this.id = id; }

    public void setNoteId(long noteId) { this.noteId = noteId; }

    public void setToken(String token) { this.token = token; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void setActive(boolean active) { this.active = active; }
}
