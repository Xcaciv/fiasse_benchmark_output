'use strict';

// Application-wide constants - centralized for modifiability
module.exports = Object.freeze({
  ROLES: {
    USER: 'user',
    ADMIN: 'admin'
  },

  VISIBILITY: {
    PRIVATE: 'private',
    PUBLIC: 'public'
  },

  // Account security limits
  AUTH: {
    BCRYPT_ROUNDS: 12,
    MAX_FAILED_LOGINS: 5,
    LOCKOUT_DURATION_MINUTES: 30,
    PASSWORD_RESET_EXPIRY_MINUTES: 60,
    MIN_PASSWORD_LENGTH: 12,
    SESSION_IDLE_MS: 30 * 60 * 1000,       // 30 minutes
    SESSION_ABSOLUTE_MS: 8 * 60 * 60 * 1000 // 8 hours
  },

  // File upload constraints
  FILES: {
    MAX_SIZE_BYTES: parseInt(process.env.MAX_FILE_SIZE_MB || '10', 10) * 1024 * 1024,
    ALLOWED_EXTENSIONS: ['.pdf', '.doc', '.docx', '.txt', '.png', '.jpg', '.jpeg'],
    ALLOWED_MIME_TYPES: [
      'application/pdf',
      'application/msword',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'text/plain',
      'image/png',
      'image/jpeg'
    ]
  },

  // Pagination
  PAGINATION: {
    DEFAULT_PAGE_SIZE: 10,
    MAX_PAGE_SIZE: 50
  },

  // Rating constraints
  RATINGS: {
    MIN_VALUE: 1,
    MAX_VALUE: 5,
    MAX_COMMENT_LENGTH: 1000,
    MIN_RATERS_FOR_TOP: 3
  },

  // Note field limits
  NOTES: {
    MAX_TITLE_LENGTH: 200,
    MAX_CONTENT_LENGTH: 50000
  }
});
