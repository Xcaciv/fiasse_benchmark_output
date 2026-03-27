'use strict';

const { AuditLog } = require('../models/index');
const { logger } = require('../config/logger');

/**
 * Enumeration of all audit action types.
 * Centralizing action names prevents typos and enables search/filter.
 */
const AUDIT_ACTIONS = Object.freeze({
  LOGIN_SUCCESS: 'LOGIN_SUCCESS',
  LOGIN_FAILURE: 'LOGIN_FAILURE',
  LOGOUT: 'LOGOUT',
  REGISTER: 'REGISTER',
  PASSWORD_RESET_REQUEST: 'PASSWORD_RESET_REQUEST',
  PASSWORD_RESET_COMPLETE: 'PASSWORD_RESET_COMPLETE',
  NOTE_CREATE: 'NOTE_CREATE',
  NOTE_UPDATE: 'NOTE_UPDATE',
  NOTE_DELETE: 'NOTE_DELETE',
  NOTE_SHARE_CREATE: 'NOTE_SHARE_CREATE',
  NOTE_SHARE_REVOKE: 'NOTE_SHARE_REVOKE',
  RATING_CREATE: 'RATING_CREATE',
  RATING_UPDATE: 'RATING_UPDATE',
  ADMIN_NOTE_REASSIGN: 'ADMIN_NOTE_REASSIGN',
  ADMIN_USER_VIEW: 'ADMIN_USER_VIEW',
  FILE_UPLOAD: 'FILE_UPLOAD',
  FILE_DELETE: 'FILE_DELETE',
});

// Fields stripped from metadata before writing to the audit log
const SENSITIVE_FIELDS = ['password', 'passwordHash', 'token', 'resetToken', 'hash', 'secret'];

/**
 * Strip sensitive fields from metadata before persisting.
 * Confidentiality: audit records must never contain credentials or tokens.
 */
const sanitizeMetadata = (metadata) => {
  if (!metadata || typeof metadata !== 'object' || Array.isArray(metadata)) {
    return metadata;
  }
  const sanitized = { ...metadata };
  for (const field of SENSITIVE_FIELDS) {
    if (field in sanitized) sanitized[field] = '[REDACTED]';
  }
  return sanitized;
};

/**
 * Write an audit record. Fire-and-forget: callers do not await.
 * Errors are logged but never propagated to preserve availability.
 *
 * @param {object} params
 * @param {string|null} params.actorId  - UUID of acting user (null for anonymous)
 * @param {string}      params.action   - AUDIT_ACTIONS constant
 * @param {string}      params.resourceType
 * @param {string|null} params.resourceId
 * @param {object}      params.metadata - will be sanitized before storage
 * @param {string}      params.ipAddress
 */
const logAction = ({ actorId, action, resourceType, resourceId, metadata, ipAddress }) => {
  const sanitized = sanitizeMetadata(metadata);

  AuditLog.create({
    actorId: actorId || null,
    action,
    resourceType,
    resourceId: resourceId || null,
    metadata: sanitized || {},
    ipAddress: ipAddress || null,
  }).catch((err) => {
    logger.error('Failed to write audit log', { error: err.message, action, resourceType });
  });
};

module.exports = { logAction, AUDIT_ACTIONS };
