package com.loosenotes.model;

import java.sql.Timestamp;

public class ShareLink {
    private int id;
    private int noteId;
    private String token;
    private Timestamp createdAt;

    public ShareLink() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNoteId() { return noteId; }
    public void setNoteId(int noteId) { this.noteId = noteId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
