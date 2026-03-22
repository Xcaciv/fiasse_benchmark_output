package com.loosenotes.model;

public class ShareLink {
    private final long id;
    private final long noteId;
    private final String createdAt;
    private final String revokedAt;

    public ShareLink(long id, long noteId, String createdAt, String revokedAt) {
        this.id = id;
        this.noteId = noteId;
        this.createdAt = createdAt;
        this.revokedAt = revokedAt;
    }

    public long getId() { return id; }
    public long getNoteId() { return noteId; }
    public String getCreatedAt() { return createdAt; }
    public String getRevokedAt() { return revokedAt; }
    public boolean isActive() { return revokedAt == null || revokedAt.isBlank(); }
}
