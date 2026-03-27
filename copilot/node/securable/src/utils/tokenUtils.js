'use strict';

const crypto = require('crypto');

/**
 * Generate a cryptographically secure random hex token (64 chars = 32 bytes).
 */
function generateToken() {
  return crypto.randomBytes(32).toString('hex');
}

/**
 * Hash a token using SHA-256 for safe storage in the database.
 * The raw token is sent to the user; only the hash is persisted.
 */
function hashToken(token) {
  return crypto.createHash('sha256').update(token).digest('hex');
}

module.exports = { generateToken, hashToken };
