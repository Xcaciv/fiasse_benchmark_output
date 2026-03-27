package com.loosenotes.service;

import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service for recording and retrieving security audit events.
 * SSEM: Accountability - every security-sensitive action is logged.
 * SSEM: Resilience - audit failures are logged but do not propagate
 *   to callers (audit must not break business operations).
 *
 * <p>NEVER pass passwords, tokens, or session IDs to log methods.
 */
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger("com.loosenotes.audit");
    private final AuditLogDao auditLogDao;

    public AuditService(AuditLogDao auditLogDao) {
        this.auditLogDao = auditLogDao;
    }

    /** Records a login success event. */
    public void logLoginSuccess(long userId, String username, String ipAddress) {
        log("LOGIN_SUCCESS", userId, null, null,
            "user=" + username, ipAddress);
    }

    /** Records a login failure event. Does NOT log the attempted password. */
    public void logLoginFailure(String username, String ipAddress) {
        log("LOGIN_FAILURE", null, null, null,
            "attempted_user=" + username, ipAddress);
    }

    /** Records a logout event. */
    public void logLogout(long userId, String username, String ipAddress) {
        log("LOGOUT", userId, null, null,
            "user=" + username, ipAddress);
    }

    /** Records a registration event. */
    public void logRegistration(long userId, String username, String ipAddress) {
        log("REGISTER", userId, null, null,
            "user=" + username, ipAddress);
    }

    /** Records a note creation. */
    public void logNoteCreated(long userId, long noteId, String ipAddress) {
        log("NOTE_CREATED", userId, "note", noteId, null, ipAddress);
    }

    /** Records a note update. */
    public void logNoteUpdated(long userId, long noteId, String ipAddress) {
        log("NOTE_UPDATED", userId, "note", noteId, null, ipAddress);
    }

    /** Records a note deletion. */
    public void logNoteDeleted(long userId, long noteId, String ipAddress) {
        log("NOTE_DELETED", userId, "note", noteId, null, ipAddress);
    }

    /** Records note ownership reassignment (admin action). */
    public void logNoteReassigned(long adminId, long noteId, long newOwnerId, String ipAddress) {
        log("NOTE_REASSIGNED", adminId, "note", noteId,
            "new_owner_id=" + newOwnerId, ipAddress);
    }

    /** Records a password reset request. Does NOT log the token. */
    public void logPasswordResetRequested(String email, String ipAddress) {
        log("PASSWORD_RESET_REQUESTED", null, null, null,
            "email_domain=" + emailDomain(email), ipAddress);
    }

    /** Records a successful password reset. */
    public void logPasswordResetCompleted(long userId, String ipAddress) {
        log("PASSWORD_RESET_COMPLETED", userId, null, null, null, ipAddress);
    }

    /** Returns recent audit entries for the admin dashboard. */
    public List<AuditLog> getRecentActivity(int limit) {
        try {
            return auditLogDao.findRecent(limit);
        } catch (Exception e) {
            log.error("Failed to retrieve audit log", e);
            return List.of();
        }
    }

    private void log(String action, Long userId, String resourceType,
                     Long resourceId, String details, String ipAddress) {
        AuditLog entry = buildEntry(action, userId, resourceType, resourceId, details, ipAddress);
        persistEntry(entry);
        logToSlf4j(entry);
    }

    private AuditLog buildEntry(String action, Long userId, String resourceType,
                                 Long resourceId, String details, String ipAddress) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setUserId(userId);
        entry.setResourceType(resourceType);
        entry.setResourceId(resourceId);
        entry.setDetails(details);
        entry.setIpAddress(ipAddress);
        return entry;
    }

    private void persistEntry(AuditLog entry) {
        try {
            auditLogDao.create(entry);
        } catch (Exception e) {
            // SSEM: Resilience - audit persistence failure must not crash the app
            log.error("Failed to persist audit log entry: action={}", entry.getAction(), e);
        }
    }

    private void logToSlf4j(AuditLog entry) {
        log.info("action={} userId={} resourceType={} resourceId={} details={} ip={}",
            entry.getAction(), entry.getUserId(), entry.getResourceType(),
            entry.getResourceId(), entry.getDetails(), entry.getIpAddress());
    }

    /** Returns only the domain part of an email for safe logging. */
    private String emailDomain(String email) {
        if (email == null) return "unknown";
        int at = email.indexOf('@');
        return at >= 0 ? email.substring(at + 1) : "unknown";
    }
}
