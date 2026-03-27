package com.loosenotes.model;

import java.time.LocalDateTime;

/**
 * Share link model.
 * tokenHash stores the SHA-256 hash of the token — the raw token value is never persisted (F-08).
 * Revocation is immediate via revokedAt timestamp (F-08, Integrity).
 */
public class ShareLink {

    private Long id;
    private Long noteId;
    private String tokenHash;   // SHA-256 of token; raw token never stored
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNoteId() { return noteId; }
    public void setNoteId(Long noteId) { this.noteId = noteId; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public boolean isRevoked() { return revokedAt != null; }
    public boolean isActive() { return revokedAt == null; }
}
