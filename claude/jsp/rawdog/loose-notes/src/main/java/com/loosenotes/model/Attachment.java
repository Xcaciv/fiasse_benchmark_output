package com.loosenotes.model;

import java.time.LocalDateTime;

public class Attachment {

    private int id;
    private int noteId;
    private String originalFilename;
    private String storedFilename;
    private long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;

    public Attachment() {}

    public Attachment(int id, int noteId, String originalFilename, String storedFilename,
                      long fileSize, String contentType, LocalDateTime uploadedAt) {
        this.id = id;
        this.noteId = noteId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.uploadedAt = uploadedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNoteId() { return noteId; }
    public void setNoteId(int noteId) { this.noteId = noteId; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getFormattedFileSize() {
        if (fileSize < 1024) return fileSize + " B";
        else if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        else return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }

    @Override
    public String toString() {
        return "Attachment{id=" + id + ", noteId=" + noteId + ", filename='" + originalFilename + "'}";
    }
}
