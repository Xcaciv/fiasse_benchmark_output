'use strict';
// App-wide constants — no secrets stored here
const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
const ALLOWED_EXTENSIONS = ['.pdf', '.doc', '.docx', '.txt', '.png', '.jpg', '.jpeg'];
const ALLOWED_MIME_TYPES = [
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/plain',
  'image/png',
  'image/jpeg'
];
const PASSWORD_RESET_TOKEN_EXPIRY_MS = 60 * 60 * 1000;
const SESSION_MAX_AGE_MS = 24 * 60 * 60 * 1000;
const BCRYPT_SALT_ROUNDS = 12;
const NOTE_EXCERPT_LENGTH = 200;
const MIN_RATINGS_FOR_TOP_RATED = 3;

module.exports = {
  MAX_FILE_SIZE_BYTES,
  ALLOWED_EXTENSIONS,
  ALLOWED_MIME_TYPES,
  PASSWORD_RESET_TOKEN_EXPIRY_MS,
  SESSION_MAX_AGE_MS,
  BCRYPT_SALT_ROUNDS,
  NOTE_EXCERPT_LENGTH,
  MIN_RATINGS_FOR_TOP_RATED
};
