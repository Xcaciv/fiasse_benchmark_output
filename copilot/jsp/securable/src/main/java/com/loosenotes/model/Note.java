package com.loosenotes.model;

import java.time.LocalDateTime;

/** Immutable domain model representing a note. */
public final class Note {

    public enum Visibility { PUBLIC, PRIVATE }

    private final long id;
    private final long userId;
    private final String title;
    private final String content;
    private final Visibility visibility;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    /** Populated via JOIN when needed; not persisted here */
    private final String authorUsername;

    public Note(long id, long userId, String title, String content,
                Visibility visibility, LocalDateTime createdAt,
                LocalDateTime updatedAt, String authorUsername) {
        this.id             = id;
        this.userId         = userId;
        this.title          = title;
        this.content        = content;
        this.visibility     = visibility;
        this.createdAt      = createdAt;
        this.updatedAt      = updatedAt;
        this.authorUsername = authorUsername;
    }

    public long getId()                  { return id; }
    public long getUserId()              { return userId; }
    public String getTitle()             { return title; }
    public String getContent()           { return content; }
    public Visibility getVisibility()    { return visibility; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getUpdatedAt()  { return updatedAt; }
    public String getAuthorUsername()    { return authorUsername; }

    public boolean isPublic() {
        return Visibility.PUBLIC.equals(visibility);
    }

    /** Returns first 200 characters as excerpt for search results. */
    public String getExcerpt() {
        if (content == null || content.isEmpty()) return "";
        return content.length() > 200 ? content.substring(0, 200) + "…" : content;
    }

    @Override
    public String toString() {
        return "Note{id=" + id + ", userId=" + userId + ", title='" + title + "'}";
    }
}
