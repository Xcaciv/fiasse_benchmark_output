package com.loosenotes.model;

import java.time.LocalDateTime;

/** Immutable domain model for a note rating. */
public final class Rating {

    private final long id;
    private final long noteId;
    private final long userId;
    private final int stars;
    private final String comment;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    /** Populated via JOIN */
    private final String raterUsername;

    public Rating(long id, long noteId, long userId, int stars,
                  String comment, LocalDateTime createdAt,
                  LocalDateTime updatedAt, String raterUsername) {
        this.id             = id;
        this.noteId         = noteId;
        this.userId         = userId;
        this.stars          = stars;
        this.comment        = comment;
        this.createdAt      = createdAt;
        this.updatedAt      = updatedAt;
        this.raterUsername  = raterUsername;
    }

    public long getId()                  { return id; }
    public long getNoteId()              { return noteId; }
    public long getUserId()              { return userId; }
    public int getStars()                { return stars; }
    public String getComment()           { return comment; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getUpdatedAt()  { return updatedAt; }
    public String getRaterUsername()     { return raterUsername; }
}
