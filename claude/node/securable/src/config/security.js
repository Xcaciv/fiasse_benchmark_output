'use strict';

/**
 * Security constants loaded from environment.
 * Centralised so no magic values are scattered across the codebase (Modifiability).
 * All values are validated at startup so misconfigurations fail fast (Resilience).
 */

const REQUIRED_VARS = ['SESSION_SECRET'];

function requireEnv(name) {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

function parsePositiveInt(name, defaultVal) {
  const raw = process.env[name];
  if (!raw) return defaultVal;
  const parsed = parseInt(raw, 10);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(`${name} must be a positive integer, got: ${raw}`);
  }
  return parsed;
}

// Fail fast on missing required vars in production
if (process.env.NODE_ENV === 'production') {
  REQUIRED_VARS.forEach(requireEnv);
}

const ALLOWED_MIME_TYPES = new Set([
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/plain',
  'image/png',
  'image/jpeg',
]);

const ALLOWED_EXTENSIONS = new Set(
  (process.env.ALLOWED_EXTENSIONS || 'pdf,doc,docx,txt,png,jpg,jpeg')
    .split(',')
    .map((e) => e.trim().toLowerCase())
);

module.exports = {
  sessionSecret: process.env.SESSION_SECRET || 'dev-only-change-in-production',
  sessionCookieMaxAge: 24 * 60 * 60 * 1000, // 24 hours
  bcryptRounds: 12,
  resetTokenExpiryMs: parsePositiveInt('RESET_TOKEN_EXPIRY_MS', 3600000),
  uploadMaxSizeBytes: parsePositiveInt('UPLOAD_MAX_SIZE_BYTES', 5 * 1024 * 1024),
  allowedMimeTypes: ALLOWED_MIME_TYPES,
  allowedExtensions: ALLOWED_EXTENSIONS,
  uploadDir: process.env.UPLOAD_DIR || './uploads',
  adminEmail: (process.env.ADMIN_EMAIL || '').toLowerCase(),

  rateLimits: {
    login: {
      windowMs: parsePositiveInt('LOGIN_RATE_LIMIT_WINDOW_MS', 15 * 60 * 1000),
      max: parsePositiveInt('LOGIN_RATE_LIMIT_MAX', 10),
    },
    general: {
      windowMs: parsePositiveInt('GENERAL_RATE_LIMIT_WINDOW_MS', 15 * 60 * 1000),
      max: parsePositiveInt('GENERAL_RATE_LIMIT_MAX', 300),
    },
  },
};
