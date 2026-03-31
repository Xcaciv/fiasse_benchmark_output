'use strict';

const { AuditLog } = require('../models');
const logger = require('../config/logger');

/**
 * Audit service — SSEM Accountability.
 * All security-relevant actions (auth, CRUD on sensitive data, admin ops) are recorded.
 * Records are append-only; this service never deletes audit log entries.
 * Sensitive data (passwords, tokens) MUST NOT be passed as metadata.
 */
async function record({ userId, action, targetType, targetId, metadata, ipAddress }) {
  try {
    await AuditLog.create({
      userId: userId || null,
      action,
      targetType: targetType || null,
      targetId: targetId ? String(targetId) : null,
      metadata: metadata || null,
      ipAddress: ipAddress || null,
    });
  } catch (err) {
    // Audit failure must not break the request — log and continue
    logger.error({ event: 'AUDIT_WRITE_FAILURE', message: err.message, action });
  }
}

module.exports = { record };
