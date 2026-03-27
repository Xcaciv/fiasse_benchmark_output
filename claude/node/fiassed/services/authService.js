'use strict';

const crypto = require('crypto');
const bcrypt = require('bcrypt');
const { v4: uuidv4 } = require('uuid');
const { User } = require('../models');
const constants = require('../config/constants');
const { logger } = require('../config/logger');

/**
 * Hash a plaintext password with bcrypt cost factor 12.
 * @param {string} password
 * @returns {Promise<string>} bcrypt hash
 */
async function hashPassword(password) {
  return bcrypt.hash(password, constants.AUTH.BCRYPT_ROUNDS);
}

/**
 * Compare a plaintext password against a stored bcrypt hash.
 * Uses constant-time comparison internally via bcrypt.
 * @param {string} password
 * @param {string} hash
 * @returns {Promise<boolean>}
 */
async function verifyPassword(password, hash) {
  return bcrypt.compare(password, hash);
}

/**
 * Validate password strength requirements.
 * Minimum 12 characters; caller should enforce additional rules in UI.
 * @param {string} password
 * @returns {{ valid: boolean, message: string }}
 */
function validatePasswordStrength(password) {
  if (!password || password.length < constants.AUTH.MIN_PASSWORD_LENGTH) {
    return {
      valid: false,
      message: `Password must be at least ${constants.AUTH.MIN_PASSWORD_LENGTH} characters`
    };
  }
  return { valid: true, message: '' };
}

/**
 * Check if an account is locked out.
 * @param {User} user - User model instance (with auth scope)
 * @returns {{ locked: boolean, until: Date|null }}
 */
function checkAccountLockout(user) {
  if (!user.lockoutUntil) return { locked: false, until: null };

  const now = new Date();
  if (user.lockoutUntil > now) {
    return { locked: true, until: user.lockoutUntil };
  }

  // Lockout expired - will be cleared on next successful login
  return { locked: false, until: null };
}

/**
 * Record a failed login attempt; apply lockout after threshold.
 * @param {User} user - User instance (with auth scope)
 */
async function recordFailedLogin(user) {
  const attempts = (user.failedLoginAttempts || 0) + 1;
  const updates = { failedLoginAttempts: attempts };

  if (attempts >= constants.AUTH.MAX_FAILED_LOGINS) {
    const lockoutUntil = new Date(
      Date.now() + constants.AUTH.LOCKOUT_DURATION_MINUTES * 60 * 1000
    );
    updates.lockoutUntil = lockoutUntil;
    logger.warn('Account locked out', {
      event: 'auth.lockout',
      actorId: user.id,
      outcome: 'failure',
      metadata: { attempts }
    });
  }

  await user.update(updates);
}

/**
 * Reset failed login counter after successful authentication.
 * @param {User} user - User instance (with auth scope)
 */
async function resetLoginAttempts(user) {
  await user.update({
    failedLoginAttempts: 0,
    lockoutUntil: null
  });
}

/**
 * Generate a password reset token.
 * Stores only the hash in the database; returns raw token for URL.
 * @returns {{ token: string, hash: string, expiresAt: Date }}
 */
function generatePasswordResetToken() {
  const token = crypto.randomBytes(32).toString('hex');
  const hash = crypto.createHash('sha256').update(token).digest('hex');
  const expiresAt = new Date(
    Date.now() + constants.AUTH.PASSWORD_RESET_EXPIRY_MINUTES * 60 * 1000
  );
  return { token, hash, expiresAt };
}

/**
 * Find user by password reset token (hash comparison).
 * Returns null if not found, expired, or already used.
 * @param {string} rawToken
 * @returns {Promise<User|null>}
 */
async function findUserByResetToken(rawToken) {
  const hash = crypto.createHash('sha256').update(rawToken).digest('hex');

  const user = await User.scope('withAuth').findOne({
    where: {
      passwordResetHash: hash,
      isActive: true
    }
  });

  if (!user) return null;
  if (!user.passwordResetExpiry || new Date() > user.passwordResetExpiry) return null;

  return user;
}

module.exports = {
  hashPassword,
  verifyPassword,
  validatePasswordStrength,
  checkAccountLockout,
  recordFailedLogin,
  resetLoginAttempts,
  generatePasswordResetToken,
  findUserByResetToken
};
