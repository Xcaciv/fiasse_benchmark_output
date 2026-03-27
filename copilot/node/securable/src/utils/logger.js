'use strict';

const { createLogger, format, transports } = require('winston');
const path = require('path');

const LOG_LEVEL = process.env.LOG_LEVEL || 'info';
const LOG_FILE = process.env.LOG_FILE || './logs/app.log';

const jsonFormat = format.combine(
  format.timestamp(),
  format.errors({ stack: true }),
  format.json()
);

const consoleFormat = format.combine(
  format.colorize(),
  format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
  format.printf(({ timestamp, level, message, ...meta }) => {
    const metaStr = Object.keys(meta).length ? ` ${JSON.stringify(meta)}` : '';
    return `${timestamp} [${level}]: ${message}${metaStr}`;
  })
);

const loggerTransports = [
  new transports.File({
    filename: LOG_FILE,
    format: jsonFormat,
    maxsize: 10 * 1024 * 1024,
    maxFiles: 5,
  }),
];

if (process.env.NODE_ENV !== 'test') {
  loggerTransports.push(
    new transports.Console({
      format: process.env.NODE_ENV === 'production' ? jsonFormat : consoleFormat,
    })
  );
}

const logger = createLogger({
  level: LOG_LEVEL,
  transports: loggerTransports,
});

module.exports = logger;
