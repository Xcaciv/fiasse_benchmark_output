'use strict';

const { ActivityLog } = require('../models');
const logger = require('../utils/logger');

/**
 * Write an activity log entry to the database and structured logger.
 * NEVER include passwords, tokens, or PII in details.
 */
async function logActivity({ userId = null, action, targetType = null, targetId = null, details = null, ipAddress = null }) {
  try {
    const detailsStr = details ? JSON.stringify(details) : null;
    await ActivityLog.create({
      userId,
      action,
      targetType,
      targetId,
      details: detailsStr,
      ipAddress,
    });
    logger.info('audit', { userId, action, targetType, targetId, ipAddress });
  } catch (err) {
    logger.error('Failed to write activity log', { action, error: err.message });
  }
}

module.exports = { logActivity };
