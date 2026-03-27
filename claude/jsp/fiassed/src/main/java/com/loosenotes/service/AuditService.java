package com.loosenotes.service;

import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.model.AuditEvent;
import com.loosenotes.util.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin facade over persistence ({@link AuditLogDao}) and structured logging
 * ({@link AuditLogger}) for audit events.
 *
 * <p><strong>Reliability / GR-03</strong>: A failure to persist an audit record
 * must never cause a user-facing operation to abort.  {@link #recordEvent} therefore
 * catches DAO exceptions, logs them at ERROR level for operator visibility, and
 * returns normally.  The {@link AuditLogger} call (log-sink) is always attempted
 * regardless of whether DAO persistence succeeded.
 *
 * <p>Constructed with an injectable {@link AuditLogDao} to support unit testing
 * and alternative DAO implementations without modifying this class (Modifiability).
 */
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogDao auditLogDao;

    /**
     * @param auditLogDao DAO responsible for durable audit-log persistence;
     *                    must not be {@code null}.
     */
    public AuditService(AuditLogDao auditLogDao) {
        if (auditLogDao == null) {
            throw new IllegalArgumentException("auditLogDao must not be null");
        }
        this.auditLogDao = auditLogDao;
    }

    /**
     * Records an audit event to both the persistent store and the structured log.
     *
     * <p>The two sinks are intentionally independent:
     * <ol>
     *   <li>Persistence via {@link AuditLogDao#append(AuditEvent)} — failure is
     *       caught and logged; it does NOT propagate to the caller.</li>
     *   <li>Structured log via {@link AuditLogger#log(AuditEvent)} — always
     *       invoked so that even a DB outage leaves a searchable trace in the
     *       application log stream.</li>
     * </ol>
     *
     * @param event the audit event to record; a {@code null} event is silently
     *              ignored (defensive; callers should never pass null)
     */
    public void recordEvent(AuditEvent event) {
        if (event == null) {
            log.warn("recordEvent called with null AuditEvent; ignoring.");
            return;
        }

        // 1. Attempt durable persistence — catch all exceptions per GR-03.
        try {
            auditLogDao.append(event);
        } catch (Exception e) {
            // Operator-visible ERROR so the gap in the audit trail is discoverable.
            log.error(
                    "Failed to persist audit event to database. "
                            + "action={} subjectId={} objectId={} error={}",
                    event.getAction(),
                    event.getSubjectId(),
                    event.getObjectId(),
                    e.getMessage(),
                    e
            );
            // Intentionally not re-thrown: the originating operation must still complete.
        }

        // 2. Always emit to the structured log sink (independent of DB success).
        try {
            AuditLogger.log(event);
        } catch (Exception e) {
            // Log-sink failures are rare but should not propagate either.
            log.error(
                    "Failed to write audit event to AuditLogger sink. "
                            + "action={} error={}",
                    event.getAction(),
                    e.getMessage(),
                    e
            );
        }
    }
}
