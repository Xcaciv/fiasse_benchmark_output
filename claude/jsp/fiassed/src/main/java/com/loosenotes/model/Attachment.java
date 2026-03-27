package com.loosenotes.model;

import java.time.LocalDateTime;

/**
 * Attachment metadata model.
 * storedFilename is a UUID-based server-generated name (F-05, Integrity).
 * originalFilename is stored only for display/download header — never used as filesystem path.
 */
public class Attachment {

    private Long id;
    private Long noteId;
    private String storedFilename;   // UUID.ext - used for filesystem path
    private String originalFilename; // User-supplied name - used only in Content-Disposition header
    private String contentType;
    private long fileSize;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNoteId() { return noteId; }
    public void setNoteId(Long noteId) { this.noteId = noteId; }

    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
