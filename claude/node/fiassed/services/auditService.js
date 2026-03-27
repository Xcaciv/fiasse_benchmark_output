'use strict';

const { v4: uuidv4 } = require('uuid');
const { logger } = require('../config/logger');

// AuditLog imported lazily to avoid circular deps at module load
let AuditLog;

function getAuditLog() {
  if (!AuditLog) {
    AuditLog = require('../models').AuditLog;
  }
  return AuditLog;
}

/**
 * Write a structured, append-only audit log entry.
 * Never throws - failures are logged to the application logger instead.
 *
 * @param {string} event - Dot-notated event name
 * @param {object} opts
 * @param {string} [opts.actorId] - UUID of acting user
 * @param {string} [opts.targetId] - UUID of affected resource
 * @param {string} [opts.targetType] - Resource type label
 * @param {string} [opts.outcome] - 'success' | 'failure' | 'denied'
 * @param {object} [opts.metadata] - Additional context (no PII, no secrets)
 * @param {string} [opts.ip] - Client IP
 * @param {string} [opts.correlationId] - Request correlation ID
 */
async function log(event, opts = {}) {
  const entry = {
    id: uuidv4(),
    event,
    actorId: opts.actorId || null,
    targetId: opts.targetId || null,
    targetType: opts.targetType || null,
    outcome: opts.outcome || 'success',
    metadata: opts.metadata || null,
    ip: opts.ip || null,
    correlationId: opts.correlationId || null
  };

  try {
    await getAuditLog().create(entry);
  } catch (dbError) {
    // Resilience: audit write failure must not crash the application
    logger.error('Audit log write failed', {
      event: 'audit.write_failure',
      originalEvent: event,
      error: dbError.message,
      correlationId: entry.correlationId
    });
  }

  // Always emit to application log as secondary audit trail
  logger.info(event, {
    event,
    actorId: entry.actorId,
    targetId: entry.targetId,
    outcome: entry.outcome,
    correlationId: entry.correlationId,
    ip: entry.ip
  });
}

module.exports = { log };
