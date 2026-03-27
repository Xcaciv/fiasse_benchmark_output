'use strict';

const winston = require('winston');

// Structured log schema: timestamp, level, event, actorId, targetId, outcome, correlationId, ip, message
const structuredFormat = winston.format.combine(
  winston.format.timestamp({ format: 'YYYY-MM-DDTHH:mm:ss.SSSZ' }),
  winston.format.errors({ stack: true }),
  winston.format.json()
);

const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: structuredFormat,
  defaultMeta: { service: 'loose-notes' },
  transports: [
    new winston.transports.Console({
      silent: process.env.NODE_ENV === 'test'
    })
  ]
});

/**
 * Create a structured security event log entry.
 * All security-sensitive operations must call this.
 * @param {string} event - Dot-notated event name (e.g. 'auth.login')
 * @param {object} fields - { actorId, targetId, targetType, outcome, correlationId, ip, metadata }
 */
function logSecurityEvent(event, fields = {}) {
  const entry = {
    event,
    actorId: fields.actorId || 'anonymous',
    targetId: fields.targetId || null,
    targetType: fields.targetType || null,
    outcome: fields.outcome || 'success',
    correlationId: fields.correlationId || null,
    ip: fields.ip || null,
    ...( fields.metadata ? { metadata: fields.metadata } : {} )
  };
  logger.info(fields.message || event, entry);
}

module.exports = { logger, logSecurityEvent };
