package com.loosenotes.model;

public class Attachment {
    private int id;
    private int noteId;
    private String originalFilename;
    private String storedFilename;
    private long fileSize;
    private String uploadedAt;

    public Attachment() {}

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

    public String getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }
}
