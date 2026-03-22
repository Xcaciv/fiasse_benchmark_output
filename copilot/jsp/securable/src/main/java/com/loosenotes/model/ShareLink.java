package com.loosenotes.model;

import java.time.LocalDateTime;

/** Immutable domain model for a share link token. */
public final class ShareLink {

    private final long id;
    private final long noteId;
    private final String token;
    private final LocalDateTime createdAt;

    public ShareLink(long id, long noteId, String token, LocalDateTime createdAt) {
        this.id        = id;
        this.noteId    = noteId;
        this.token     = token;
        this.createdAt = createdAt;
    }

    public long getId()               { return id; }
    public long getNoteId()           { return noteId; }
    public String getToken()          { return token; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
}
