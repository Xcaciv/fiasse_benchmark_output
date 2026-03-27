'use strict';

const { createLogger, format, transports } = require('winston');
const path = require('path');
const fs = require('fs');

const logDir = path.resolve('./logs');
if (!fs.existsSync(logDir)) {
  fs.mkdirSync(logDir, { recursive: true });
}

// Structured logging — Accountability: audit trail without sensitive data
const logger = createLogger({
  level: process.env.LOG_LEVEL || (process.env.NODE_ENV === 'production' ? 'info' : 'debug'),
  format: format.combine(
    format.timestamp(),
    format.errors({ stack: true }),
    format.json()
  ),
  defaultMeta: { service: 'loose-notes' },
  transports: [
    new transports.File({
      filename: path.join(logDir, 'error.log'),
      level: 'error',
      maxsize: 5 * 1024 * 1024, // 5 MB — Availability: prevent disk exhaustion
      maxFiles: 5,
    }),
    new transports.File({
      filename: path.join(logDir, 'combined.log'),
      maxsize: 10 * 1024 * 1024,
      maxFiles: 10,
    }),
  ],
});

if (process.env.NODE_ENV !== 'production') {
  logger.add(new transports.Console({
    format: format.combine(format.colorize(), format.simple()),
  }));
}

module.exports = logger;
