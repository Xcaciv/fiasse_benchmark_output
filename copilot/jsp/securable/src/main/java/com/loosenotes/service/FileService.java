package com.loosenotes.service;

import com.loosenotes.model.Attachment;
import java.io.InputStream;
import java.io.OutputStream;

/** Business logic contract for file storage (Availability, Integrity). */
public interface FileService {
    /**
     * Stores a file stream and returns the stored (unique) filename.
     * Validates size limit before writing (Availability: resource limits).
     */
    String store(String originalFilename, InputStream inputStream, long fileSize);

    /**
     * Writes the stored file to the output stream.
     * Caller is responsible for closing streams.
     */
    void retrieve(String storedFilename, OutputStream outputStream);

    /** Deletes the stored file; returns true if deleted. */
    boolean delete(String storedFilename);
}
