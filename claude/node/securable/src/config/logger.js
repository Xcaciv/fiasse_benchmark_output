'use strict';

/**
 * Structured logger — SSEM Transparency / Accountability.
 * All log entries are JSON so they can be parsed by log aggregators.
 * Sensitive data (passwords, tokens, PII) MUST NOT be passed to any logger call.
 */

const { createLogger, format, transports } = require('winston');

const { combine, timestamp, json, errors, colorize, simple } = format;

const isDev = process.env.NODE_ENV !== 'production';

const logger = createLogger({
  level: isDev ? 'debug' : 'info',
  format: combine(
    errors({ stack: true }),
    timestamp(),
    json()
  ),
  defaultMeta: { service: 'loose-notes' },
  transports: [
    new transports.Console({
      format: isDev ? combine(colorize(), simple()) : combine(timestamp(), json()),
    }),
  ],
  exitOnError: false,
});

/**
 * Log a security audit event.
 * @param {string} action  - e.g. 'LOGIN_SUCCESS', 'NOTE_DELETE'
 * @param {object} context - { userId, targetId, ip, ... } — no secrets
 */
logger.audit = (action, context = {}) => {
  logger.info({ event: 'AUDIT', action, ...context });
};

module.exports = logger;
