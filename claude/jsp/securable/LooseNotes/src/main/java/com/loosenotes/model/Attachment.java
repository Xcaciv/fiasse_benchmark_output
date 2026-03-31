package com.loosenotes.model;

import java.time.Instant;

/**
 * Represents a file attachment linked to a note.
 *
 * SSEM notes:
 * - Confidentiality: storedName is a UUID, never the originalName.
 *   originalName is display-only and output-encoded in JSP.
 * - Integrity: fileSizeBytes enforces upload limits at the model level.
 */
public class Attachment {

    private long id;
    private long noteId;
    /** User-supplied filename – must be HTML-encoded on display. */
    private String originalName;
    /** UUID-based server-generated filename – prevents path traversal and collisions. */
    private String storedName;
    private String mimeType;
    private long fileSizeBytes;
    private Instant uploadedAt;

    public Attachment() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getNoteId() { return noteId; }
    public void setNoteId(long noteId) { this.noteId = noteId; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

    @Override
    public String toString() {
        return "Attachment{id=" + id + ", noteId=" + noteId + ", originalName='" + originalName + "'}";
    }
}
