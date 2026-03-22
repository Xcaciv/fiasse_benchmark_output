package com.loosenotes.dao;

import com.loosenotes.model.ShareLink;
import java.util.Optional;

/** Data access contract for note share links. */
public interface ShareLinkDao {
    long insert(ShareLink link);
    Optional<ShareLink> findByNoteId(long noteId);
    Optional<ShareLink> findByToken(String token);
    boolean deleteByNoteId(long noteId);
}
