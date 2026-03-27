'use strict';

// Centralized security configuration — Modifiability: one place to update policies
const ALLOWED_MIME_TYPES = new Set([
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/plain',
  'image/png',
  'image/jpeg',
]);

const ALLOWED_EXTENSIONS = new Set(['.pdf', '.doc', '.docx', '.txt', '.png', '.jpg', '.jpeg']);

const MAX_FILE_SIZE_BYTES = (parseInt(process.env.MAX_FILE_SIZE_MB, 10) || 10) * 1024 * 1024;

const PASSWORD_MIN_LENGTH = 8;
const PASSWORD_MAX_LENGTH = 128;

const USERNAME_MIN_LENGTH = 3;
const USERNAME_MAX_LENGTH = 32;

const NOTE_TITLE_MAX_LENGTH = 200;
const NOTE_CONTENT_MAX_LENGTH = 50000;

const RESET_TOKEN_TTL_MS = (parseInt(process.env.RESET_TOKEN_TTL_SECONDS, 10) || 3600) * 1000;

const BCRYPT_ROUNDS = 12;

const SESSION_COOKIE_OPTIONS = {
  httpOnly: true,              // Prevent JS access — Confidentiality
  secure: process.env.NODE_ENV === 'production', // HTTPS only in prod
  sameSite: 'lax',             // CSRF mitigation
  maxAge: 8 * 60 * 60 * 1000, // 8 hours
};

module.exports = {
  ALLOWED_MIME_TYPES,
  ALLOWED_EXTENSIONS,
  MAX_FILE_SIZE_BYTES,
  PASSWORD_MIN_LENGTH,
  PASSWORD_MAX_LENGTH,
  USERNAME_MIN_LENGTH,
  USERNAME_MAX_LENGTH,
  NOTE_TITLE_MAX_LENGTH,
  NOTE_CONTENT_MAX_LENGTH,
  RESET_TOKEN_TTL_MS,
  BCRYPT_ROUNDS,
  SESSION_COOKIE_OPTIONS,
};
