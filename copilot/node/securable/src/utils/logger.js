'use strict';
const winston = require('winston');
const path = require('path');

// Accountability: Structured logging — never log passwords, tokens, or raw user content
const transports = [
  new winston.transports.Console({
    format: winston.format.combine(
      winston.format.colorize(),
      winston.format.simple()
    )
  }),
  new winston.transports.File({
    filename: path.join('logs', 'app.log'),
    format: winston.format.combine(
      winston.format.timestamp(),
      winston.format.json()
    )
  })
];

const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  transports
});

module.exports = logger;
