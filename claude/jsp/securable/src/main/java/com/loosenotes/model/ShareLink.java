package com.loosenotes.model;

import java.time.LocalDateTime;

public class ShareLink {
    private long id;
    private long noteId;
    private String token;
    private LocalDateTime createdAt;

    public ShareLink() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getNoteId() { return noteId; }
    public void setNoteId(long noteId) { this.noteId = noteId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
