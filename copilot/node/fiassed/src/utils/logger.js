'use strict';
const winston = require('winston');
const path = require('path');
const config = require('../config');
const { mkdirSync } = require('fs');

mkdirSync(config.logsPath, { recursive: true });

const logger = winston.createLogger({
  level: config.nodeEnv === 'production' ? 'info' : 'debug',
  format: winston.format.combine(
    winston.format.timestamp({ format: 'YYYY-MM-DDTHH:mm:ss.SSSZ' }),
    winston.format.errors({ stack: true }),
    winston.format.json()
  ),
  transports: [
    new winston.transports.File({
      filename: path.join(config.logsPath, 'app.log'),
      maxsize: 10 * 1024 * 1024,
      maxFiles: 10,
      tailable: true,
    }),
    new winston.transports.File({
      filename: path.join(config.logsPath, 'audit.log'),
      level: 'info',
      maxsize: 10 * 1024 * 1024,
      maxFiles: 30,
      tailable: true,
    }),
  ],
});

if (config.nodeEnv !== 'production') {
  logger.add(new winston.transports.Console({
    format: winston.format.combine(
      winston.format.colorize(),
      winston.format.simple()
    ),
  }));
}

module.exports = logger;
