'use strict';
const { v4: uuidv4 } = require('uuid');
const logger = require('../utils/logger');
const auditLogModel = require('../models/auditLogModel');

let _db = null;

function init(db) {
  _db = db;
}

function log({ eventType, userId, ipAddress, resourceType, resourceId, details }) {
  const entry = {
    id: uuidv4(),
    eventType,
    userId: userId || null,
    ipAddress: ipAddress || null,
    resourceType: resourceType || null,
    resourceId: resourceId || null,
    details: details || null,
  };

  // Always log to Winston for structured log output
  logger.info('AUDIT', {
    event_type: entry.eventType,
    user_id: entry.userId,
    ip_address: entry.ipAddress,
    resource_type: entry.resourceType,
    resource_id: entry.resourceId,
    details: entry.details,
  });

  // Also persist to database if initialized
  if (_db) {
    try {
      auditLogModel.createAuditLog(_db, entry);
    } catch (err) {
      logger.error('Failed to persist audit log to database', { error: err.message, eventType });
    }
  }
}

module.exports = { init, log };
