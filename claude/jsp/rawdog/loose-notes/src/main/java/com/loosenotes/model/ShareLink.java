package com.loosenotes.model;

import java.time.LocalDateTime;

public class ShareLink {

    private int id;
    private int noteId;
    private String token;
    private LocalDateTime createdAt;

    public ShareLink() {}

    public ShareLink(int id, int noteId, String token, LocalDateTime createdAt) {
        this.id = id;
        this.noteId = noteId;
        this.token = token;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNoteId() { return noteId; }
    public void setNoteId(int noteId) { this.noteId = noteId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "ShareLink{id=" + id + ", noteId=" + noteId + ", token='" + token + "'}";
    }
}
