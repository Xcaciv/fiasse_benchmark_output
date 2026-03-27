'use strict';

const { AuditLog } = require('../models');
const logger = require('../config/logger');

// Accountability service — all security-sensitive actions are recorded here.
// IMPORTANT: never include passwords, tokens, or PII in metadata.
async function record(action, userId, metadata = {}, ipAddress = null) {
  try {
    await AuditLog.create({ action, userId: userId || null, metadata, ipAddress });
    logger.info('Audit', { action, userId, ...metadata });
  } catch (err) {
    // Audit failure must not break the primary operation — Resilience
    logger.error('Audit log write failed', { action, error: err.message });
  }
}

module.exports = { record };
