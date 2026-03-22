'use strict';

// Accountability: Structured audit log writer — injected logger for testability
function createAuditService(logger) {
  function logAuthEvent(action, userId, username, ipAddress, success, details = {}) {
    // Never log passwords or tokens — only safe metadata
    logger.info('AUTH_EVENT', {
      action,
      userId,
      username,
      ipAddress,
      success,
      details,
      timestamp: new Date().toISOString()
    });
  }

  function logAdminAction(action, adminId, targetId, details = {}) {
    logger.info('ADMIN_ACTION', {
      action,
      adminId,
      targetId,
      details,
      timestamp: new Date().toISOString()
    });
  }

  function logNoteAction(action, userId, noteId, details = {}) {
    logger.info('NOTE_ACTION', {
      action,
      userId,
      noteId,
      details,
      timestamp: new Date().toISOString()
    });
  }

  return { logAuthEvent, logAdminAction, logNoteAction };
}

module.exports = { createAuditService };
