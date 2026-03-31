package com.loosenotes.service;

import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.model.AuditLog;
import com.loosenotes.model.AuditLog.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Records security-sensitive actions to the audit trail.
 *
 * SSEM / ASVS alignment:
 * - ASVS V7.2 (Log Events): all auth and admin actions logged.
 * - Accountability: structured "who/what/where" format on every entry.
 * - Confidentiality: ipAddress is anonymized before storage (/24 subnet).
 * - Resilience: logging failure is non-fatal – a warning is emitted but the
 *   primary operation is not blocked.
 */
public class AuditService {

    private static final Logger log      = LoggerFactory.getLogger(AuditService.class);
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final AuditLogDao auditLogDao;

    public AuditService(AuditLogDao auditLogDao) {
        this.auditLogDao = auditLogDao;
    }

    /**
     * Records an audit event.
     *
     * @param userId      the acting user ID (null for unauthenticated actions)
     * @param eventType   category of the event
     * @param detail      human-readable description (no PII, no passwords)
     * @param rawIpAddress the client IP, anonymized before storage
     */
    public void record(Long userId, EventType eventType, String detail, String rawIpAddress) {
        String anonymizedIp = anonymizeIp(rawIpAddress);
        // Write structured line to dedicated audit log file
        auditLog.info("type={} userId={} ip={} detail={}",
                eventType, userId != null ? userId : "anon", anonymizedIp, detail);
        // Persist to database for admin dashboard
        AuditLog entry = new AuditLog();
        entry.setUserId(userId);
        entry.setEventType(eventType);
        entry.setEventDetail(detail);
        entry.setIpAddress(anonymizedIp);
        try {
            auditLogDao.insert(entry);
        } catch (SQLException e) {
            // Non-fatal: log warning but do not propagate – audit must not block ops
            log.warn("Failed to persist audit log entry: type={}, userId={}", eventType, userId, e);
        }
    }

    /** Returns the most recent {@code limit} audit entries for the admin dashboard. */
    public List<AuditLog> getRecentActivity(int limit) throws SQLException {
        return auditLogDao.findRecent(Math.min(limit, 200));
    }

    /**
     * Anonymizes an IPv4 address to its /24 prefix (e.g., 192.168.1.x).
     * IPv6 addresses are returned as-is (shortened) to avoid identifying data.
     * Null input returns "unknown".
     */
    private String anonymizeIp(String rawIp) {
        if (rawIp == null || rawIp.isBlank()) return "unknown";
        if (rawIp.contains(":")) {
            // IPv6 – truncate to first 4 groups
            String[] parts = rawIp.split(":");
            int groupsToKeep = Math.min(parts.length, 4);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < groupsToKeep; i++) {
                if (i > 0) sb.append(':');
                sb.append(parts[i]);
            }
            return sb + "::/48";
        }
        // IPv4 – keep first three octets only
        int lastDot = rawIp.lastIndexOf('.');
        return lastDot > 0 ? rawIp.substring(0, lastDot) + ".x" : rawIp;
    }
}
