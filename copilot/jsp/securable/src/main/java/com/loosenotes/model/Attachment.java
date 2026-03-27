package com.loosenotes.model;

/**
 * Represents a file attached to a note.
 * storedFilename is a UUID-based name; originalFilename is for display only.
 */
public class Attachment {

    private long id;
    private long noteId;
    private String originalFilename;
    private String storedFilename;
    private String contentType;
    private long fileSize;
    private long uploadedAt;

    public Attachment() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getNoteId() { return noteId; }
    public void setNoteId(long noteId) { this.noteId = noteId; }

    /** Display name — must be escaped before rendering. */
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    /** Server-controlled UUID filename — not exposed to the user as a path. */
    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public long getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(long uploadedAt) { this.uploadedAt = uploadedAt; }
}
