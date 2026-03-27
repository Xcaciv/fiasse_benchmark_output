package com.loosenotes.model;

import java.time.LocalDateTime;

/**
 * Represents a file attachment for a note.
 * SSEM: Integrity - stored filename is system-generated, original is metadata only.
 * SSEM: Confidentiality - storedFilename must not be exposed to users directly.
 */
public class Attachment {

    private long id;
    private long noteId;
    /** User-provided original filename - sanitized before storage. */
    private String originalFilename;
    /** System-generated UUID filename - never expose to users. */
    private String storedFilename;
    private long fileSize;
    private String contentType;
    private LocalDateTime createdAt;

    public Attachment() {}

    // ---- Getters ----

    public long getId() { return id; }

    public long getNoteId() { return noteId; }

    public String getOriginalFilename() { return originalFilename; }

    /** Internal use only - do not expose in views or responses. */
    public String getStoredFilename() { return storedFilename; }

    public long getFileSize() { return fileSize; }

    public String getContentType() { return contentType; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    /** Returns human-readable file size string. */
    public String getFormattedFileSize() {
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }

    // ---- Setters ----

    public void setId(long id) { this.id = id; }

    public void setNoteId(long noteId) { this.noteId = noteId; }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public void setContentType(String contentType) { this.contentType = contentType; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
