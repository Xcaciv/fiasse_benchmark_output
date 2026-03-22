package com.loosenotes.model;

public class Attachment {
    private final long id;
    private final long noteId;
    private final String storedName;
    private final String originalName;
    private final String contentType;
    private final long sizeBytes;
    private final String createdAt;

    public Attachment(long id, long noteId, String storedName, String originalName, String contentType, long sizeBytes, String createdAt) {
        this.id = id;
        this.noteId = noteId;
        this.storedName = storedName;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public long getNoteId() { return noteId; }
    public String getStoredName() { return storedName; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getCreatedAt() { return createdAt; }
}
