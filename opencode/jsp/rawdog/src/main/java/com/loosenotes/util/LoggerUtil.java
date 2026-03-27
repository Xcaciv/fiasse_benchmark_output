package com.loosenotes.util;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.model.ActivityLog;
import javax.servlet.http.HttpServletRequest;

public class LoggerUtil {

    private static final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public static void logLogin(Long userId, HttpServletRequest request) {
        logActivity(userId, ActivityLog.Actions.LOGIN, ActivityLog.Entities.USER, userId, 
                    "User logged in", request);
    }

    public static void logLogout(Long userId, HttpServletRequest request) {
        logActivity(userId, ActivityLog.Actions.LOGOUT, ActivityLog.Entities.USER, userId, 
                    "User logged out", request);
    }

    public static void logLoginFailed(String username, HttpServletRequest request) {
        ActivityLog log = new ActivityLog(null, ActivityLog.Actions.LOGIN_FAILED, 
                                           ActivityLog.Entities.USER, null, 
                                           "Failed login attempt for user: " + username, request);
        log.setIpAddress(SessionUtil.getClientIp(request));
        activityLogDAO.logActivity(log);
    }

    public static void logRegister(Long userId, String username, HttpServletRequest request) {
        logActivity(userId, ActivityLog.Actions.REGISTER, ActivityLog.Entities.USER, userId, 
                    "User registered: " + username, request);
    }

    public static void logNoteCreate(Long userId, Long noteId, String title, HttpServletRequest request) {
        logActivity(userId, ActivityLog.Actions.NOTE_CREATE, ActivityLog.Entities.NOTE, noteId, 
                    "Note created: " + title, request);
    }

    public static void logNoteUpdate(Long userId, Long noteId, String title, HttpServletRequest request) {
        logActivity(userId, ActivityLog.Actions.NOTE_UPDATE, ActivityLog.Entities.NOTE, noteId, 
                    "Note updated: " + title, request);
    }

    public static void logNoteDelete(Long userId, Long noteId, String title, HttpServletRequest request) {
        logActivity(userId, ActivityLog.Actions.NOTE_DELETE, ActivityLog.Entities.NOTE, noteId, 
                    "Note deleted: " + title, request);
    }

    public static void logNoteShare(Long userId, Long noteId, String title, HttpServletRequest request) {
        logActivity(userId, ActivityLog.Actions.NOTE_SHARE, ActivityLog.Entities.NOTE, noteId, 
                    "Note shared: " + title, request);
    }

    public static void logRatingCreate(Long userId, Long noteId, int rating, HttpServletRequest request) {
        logActivity(userId, ActivityLog.Actions.RATING_CREATE, ActivityLog.Entities.RATING, noteId, 
                    "Rating added: " + rating + " stars", request);
    }

    public static void logRatingUpdate(Long userId, Long noteId, int rating, HttpServletRequest request) {
        logActivity(userId, ActivityLog.Actions.RATING_UPDATE, ActivityLog.Entities.RATING, noteId, 
                    "Rating updated: " + rating + " stars", request);
    }

    public static void logFileUpload(Long userId, Long noteId, String filename, HttpServletRequest request) {
        logActivity(userId, ActivityLog.Actions.FILE_UPLOAD, ActivityLog.Entities.ATTACHMENT, noteId, 
                    "File uploaded: " + filename, request);
    }

    public static void logPasswordResetRequest(String email, HttpServletRequest request) {
        ActivityLog log = new ActivityLog(null, ActivityLog.Actions.PASSWORD_RESET_REQUEST, 
                                           ActivityLog.Entities.USER, null, 
                                           "Password reset requested for: " + email, request);
        log.setIpAddress(SessionUtil.getClientIp(request));
        activityLogDAO.logActivity(log);
    }

    public static void logPasswordReset(Long userId, HttpServletRequest request) {
        logActivity(userId, ActivityLog.Actions.PASSWORD_RESET, ActivityLog.Entities.USER, userId, 
                    "Password reset completed", request);
    }

    public static void logAdminNoteReassign(Long adminId, Long noteId, Long newOwnerId, HttpServletRequest request) {
        String details = String.format("Note %d reassigned to user %d", noteId, newOwnerId);
        logActivity(adminId, ActivityLog.Actions.ADMIN_NOTE_REASSIGN, ActivityLog.Entities.NOTE, noteId, 
                    details, request);
    }

    public static void logAdminUserManagement(Long adminId, Long targetUserId, String action, HttpServletRequest request) {
        String details = String.format("User %d: %s", targetUserId, action);
        logActivity(adminId, ActivityLog.Actions.ADMIN_USER_MANAGEMENT, ActivityLog.Entities.USER, targetUserId, 
                    details, request);
    }

    private static void logActivity(Long userId, String action, String entityType, Long entityId, 
                                    String details, HttpServletRequest request) {
        ActivityLog log = new ActivityLog(userId, action, entityType, entityId, details, 
                                          SessionUtil.getClientIp(request));
        activityLogDAO.logActivity(log);
    }
}
