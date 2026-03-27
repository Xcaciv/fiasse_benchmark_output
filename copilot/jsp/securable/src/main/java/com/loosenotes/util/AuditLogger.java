package com.loosenotes.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Structured audit logging for security-relevant events.
 *
 * FIASSE Accountability: every log entry contains who/what/when/where.
 * No PII (email, password, tokens) is written to the audit log — only identifiers and actions.
 * The AUDIT logger routes to a separate appender (see logback.xml).
 */
public final class AuditLogger {

    /** Dedicated logger routed to the audit appender. */
    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private AuditLogger() {}

    /**
     * Logs authentication events: LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT, REGISTER.
     * No password or email logged.
     */
    public static void logAuthEvent(String eventType, String username,
                                    String ipAddress, String details) {
        AUDIT.info("event={} username={} ip={} details={}",
                   sanitize(eventType), sanitize(username),
                   sanitize(ipAddress), sanitize(details));
    }

    /**
     * Logs admin actions on any resource.
     */
    public static void logAdminAction(String action, long adminId, String adminUsername,
                                      String targetType, String targetId, String ipAddress) {
        AUDIT.info("event=ADMIN_ACTION action={} adminId={} adminUsername={} "
                   + "targetType={} targetId={} ip={}",
                   sanitize(action), adminId, sanitize(adminUsername),
                   sanitize(targetType), sanitize(targetId), sanitize(ipAddress));
    }

    /**
     * Logs note lifecycle actions: CREATE, EDIT, DELETE, SHARE, REVOKE_SHARE, TOGGLE_PUBLIC.
     */
    public static void logNoteAction(String action, long userId, String username,
                                     long noteId, String ipAddress) {
        AUDIT.info("event=NOTE_ACTION action={} userId={} username={} noteId={} ip={}",
                   sanitize(action), userId, sanitize(username), noteId, sanitize(ipAddress));
    }

    /**
     * Logs generic security events: CSRF_VIOLATION, ACCESS_DENIED, INVALID_INPUT, etc.
     */
    public static void logSecurityEvent(String event, String ipAddress, String details) {
        AUDIT.info("event=SECURITY_EVENT type={} ip={} details={}",
                   sanitize(event), sanitize(ipAddress), sanitize(details));
    }

    /** Replaces null with "-" and strips newlines to prevent log injection. */
    private static String sanitize(String value) {
        if (value == null) {
            return "-";
        }
        return value.replace('\n', '_').replace('\r', '_');
    }
}
