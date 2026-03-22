package com.loosenotes.dao;

import com.loosenotes.model.AuditLogEntry;
import java.util.List;

/** Data access contract for audit log entries (Accountability). */
public interface AuditLogDao {
    void insert(AuditLogEntry entry);
    List<AuditLogEntry> findRecent(int limit);
}
