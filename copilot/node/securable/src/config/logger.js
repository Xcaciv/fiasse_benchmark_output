'use strict';

const winston = require('winston');

const { combine, timestamp, json, printf, colorize } = winston.format;

// Fields that must never appear in logs
const SENSITIVE_FIELDS = ['password', 'passwordHash', 'token', 'resetToken', 'hash', 'secret'];

/**
 * Remove sensitive fields from log metadata before writing.
 * Applied at trust boundary: log call → transport write.
 */
const sanitizeMetadata = (meta) => {
  if (!meta || typeof meta !== 'object' || Array.isArray(meta)) return meta;
  const sanitized = { ...meta };
  for (const field of SENSITIVE_FIELDS) {
    if (field in sanitized) sanitized[field] = '[REDACTED]';
  }
  return sanitized;
};

const sanitizeFormat = winston.format((info) => {
  const { level, message, timestamp: ts, ...rest } = info;
  return { level, message, timestamp: ts, ...sanitizeMetadata(rest) };
});

const developmentFormat = combine(
  sanitizeFormat(),
  colorize(),
  timestamp(),
  printf(({ level, message, timestamp: ts, ...meta }) => {
    const extras = Object.keys(meta).length ? ` ${JSON.stringify(meta)}` : '';
    return `${ts} [${level}] ${message}${extras}`;
  })
);

const productionFormat = combine(
  sanitizeFormat(),
  timestamp(),
  json()
);

const isProduction = process.env.NODE_ENV === 'production';

const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  transports: [
    new winston.transports.Console({
      format: isProduction ? productionFormat : developmentFormat,
    }),
    new winston.transports.File({
      filename: 'logs/error.log',
      level: 'error',
      format: combine(sanitizeFormat(), timestamp(), json()),
    }),
    new winston.transports.File({
      filename: 'logs/app.log',
      format: combine(sanitizeFormat(), timestamp(), json()),
    }),
  ],
});

module.exports = { logger };
