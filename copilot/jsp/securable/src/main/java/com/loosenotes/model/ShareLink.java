package com.loosenotes.model;

/**
 * A unique token-based share link for a note.
 * Anyone with the token can view the note without authentication.
 */
public class ShareLink {

    private long id;
    private long noteId;
    private String token;
    private long createdAt;

    public ShareLink() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getNoteId() { return noteId; }
    public void setNoteId(long noteId) { this.noteId = noteId; }

    /** Cryptographically random 64-hex-char token. */
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
