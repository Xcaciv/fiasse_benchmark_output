package com.loosenotes.dao;

import com.loosenotes.model.Attachment;
import java.util.List;
import java.util.Optional;

/** Data access contract for file attachments. */
public interface AttachmentDao {
    long insert(Attachment attachment);
    Optional<Attachment> findById(long id);
    List<Attachment> findByNoteId(long noteId);
    boolean delete(long id);
}
