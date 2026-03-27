'use strict';

const path = require('path');

const config = {
  port: parseInt(process.env.PORT || '3000', 10),
  nodeEnv: process.env.NODE_ENV || 'development',
  sessionSecret: process.env.SESSION_SECRET || (() => {
    if (process.env.NODE_ENV === 'production') {
      throw new Error('SESSION_SECRET must be set in production');
    }
    return 'dev-secret-change-in-production-must-be-32-chars-min';
  })(),
  sessionMaxAge: parseInt(process.env.SESSION_MAX_AGE_MS || String(30 * 60 * 1000), 10),
  dbPath: process.env.DB_PATH || path.join(__dirname, '../../data/loose-notes.db'),
  uploadsPath: process.env.UPLOADS_PATH || path.join(__dirname, '../../uploads'),
  logsPath: process.env.LOGS_PATH || path.join(__dirname, '../../logs'),
  maxFileSize: parseInt(process.env.MAX_FILE_SIZE_BYTES || String(10 * 1024 * 1024), 10),
  passwordMinLength: 12,
  passwordMaxLength: 128,
  noteTitleMaxLength: 255,
  noteContentMaxLength: 65535,
  searchQueryMaxLength: 200,
  ratingCommentMaxLength: 1000,
  usernameMaxLength: 64,
  resetTokenExpiryMs: 60 * 60 * 1000,
  loginRateLimitWindowMs: 15 * 60 * 1000,
  loginRateLimitMax: 5,
  searchRateLimitWindowMs: 60 * 1000,
  searchRateLimitMax: 30,
  generalRateLimitWindowMs: 15 * 60 * 1000,
  generalRateLimitMax: 100,
  allowedMimeTypes: {
    'application/pdf': [0x25, 0x50, 0x44, 0x46],
    'application/msword': [0xD0, 0xCF, 0x11, 0xE0],
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document': [0x50, 0x4B],
    'text/plain': null,
    'image/png': [0x89, 0x50, 0x4E, 0x47],
    'image/jpeg': [0xFF, 0xD8, 0xFF],
  },
  allowedExtensions: ['.pdf', '.doc', '.docx', '.txt', '.png', '.jpg', '.jpeg'],
  forbiddenMimeTypes: [
    'application/zip',
    'application/x-tar',
    'application/gzip',
    'application/x-rar-compressed',
    'application/x-7z-compressed',
  ],
};

module.exports = config;
