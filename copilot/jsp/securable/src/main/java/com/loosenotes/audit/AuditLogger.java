package com.loosenotes.audit;

import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.model.AuditLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Centralized structured audit logging (Accountability).
 * Writes to the audit_log table AND to the AUDIT SLF4J logger.
 * Never logs sensitive data (passwords, tokens, session IDs).
 */
public final class AuditLogger {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private final AuditLogDao auditLogDao;

    public AuditLogger(AuditLogDao auditLogDao) {
        this.auditLogDao = auditLogDao;
    }

    /**
     * Records a security-relevant event.
     *
     * @param actorId      user ID performing the action (null for anonymous)
     * @param actorName    username (never a password or token)
     * @param action       action identifier, e.g. "LOGIN_SUCCESS"
     * @param resourceType type of resource affected, e.g. "NOTE"
     * @param resourceId   ID of affected resource (may be null)
     * @param ip           client IP address
     * @param outcome      "SUCCESS" or "FAILURE"
     * @param detail       non-sensitive contextual detail
     */
    public void log(Long actorId, String actorName, String action,
                    String resourceType, String resourceId,
                    String ip, String outcome, String detail) {
        String safeName = actorName != null ? actorName : "anonymous";
        String safeIp   = ip != null ? ip : "unknown";

        // Structured log line for SIEM ingestion
        AUDIT.info("action={} actor={} resourceType={} resourceId={} ip={} outcome={} detail={}",
                action, safeName, resourceType, resourceId, safeIp, outcome, detail);

        persistEntry(actorId, safeName, action, resourceType, resourceId,
                safeIp, outcome, detail);
    }

    private void persistEntry(Long actorId, String actorName, String action,
                               String resourceType, String resourceId,
                               String ip, String outcome, String detail) {
        try {
            AuditLogEntry entry = new AuditLogEntry(
                0, actorId, actorName, action, resourceType, resourceId,
                ip, outcome, detail, LocalDateTime.now()
            );
            auditLogDao.insert(entry);
        } catch (Exception e) {
            // Audit persistence failure must not break the request (Resilience)
            AUDIT.error("Failed to persist audit entry action={} actor={}", action, actorName, e);
        }
    }
}
