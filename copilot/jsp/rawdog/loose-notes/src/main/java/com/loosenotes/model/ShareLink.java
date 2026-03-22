package com.loosenotes.model;

public class ShareLink {
    private int id;
    private int noteId;
    private String token;
    private String createdAt;

    public ShareLink() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNoteId() { return noteId; }
    public void setNoteId(int noteId) { this.noteId = noteId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
