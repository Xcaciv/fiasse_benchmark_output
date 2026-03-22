package com.loosenotes.model;

import java.time.LocalDateTime;

/** Immutable domain model representing a file attached to a note. */
public final class Attachment {

    private final long id;
    private final long noteId;
    private final String originalFilename;
    private final String storedFilename;
    private final long fileSize;
    private final String mimeType;
    private final LocalDateTime uploadedAt;

    public Attachment(long id, long noteId, String originalFilename,
                      String storedFilename, long fileSize,
                      String mimeType, LocalDateTime uploadedAt) {
        this.id               = id;
        this.noteId           = noteId;
        this.originalFilename = originalFilename;
        this.storedFilename   = storedFilename;
        this.fileSize         = fileSize;
        this.mimeType         = mimeType;
        this.uploadedAt       = uploadedAt;
    }

    public long getId()                   { return id; }
    public long getNoteId()               { return noteId; }
    public String getOriginalFilename()   { return originalFilename; }
    public String getStoredFilename()     { return storedFilename; }
    public long getFileSize()             { return fileSize; }
    public String getMimeType()           { return mimeType; }
    public LocalDateTime getUploadedAt()  { return uploadedAt; }
}
