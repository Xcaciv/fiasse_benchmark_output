'use strict';
const crypto = require('crypto');

// Authenticity: Cryptographically secure token generation
function generateSecureToken(byteLength = 32) {
  return crypto.randomBytes(byteLength).toString('hex');
}

// Integrity: Hash token before storage — never store plaintext tokens
function hashToken(token) {
  return crypto.createHash('sha256').update(token).digest('hex');
}

module.exports = { generateSecureToken, hashToken };
