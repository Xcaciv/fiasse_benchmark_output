'use strict';

const { Op } = require('sequelize');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');
const { User } = require('../models/index');
const { logAction, AUDIT_ACTIONS } = require('./auditService');
const { logger } = require('../config/logger');

const TOKEN_EXPIRY_MS = 60 * 60 * 1000; // 1 hour

/**
 * Register a new user. First registered user becomes admin (seed mechanism).
 * Trust boundary: all fields validated by middleware before reaching here.
 */
const registerUser = async ({ username, email, password, ipAddress }) => {
  const userCount = await User.count();
  const role = userCount === 0 ? 'admin' : 'user';

  const passwordHash = await User.hashPassword(password);

  const user = await User.create({ username, email, passwordHash, role });

  logAction({
    actorId: user.id,
    action: AUDIT_ACTIONS.REGISTER,
    resourceType: 'User',
    resourceId: user.id,
    metadata: { username, role },
    ipAddress,
  });

  return user;
};

/**
 * Authenticate a user. Timing-safe: always runs bcrypt.compare regardless of
 * whether the user exists, preventing user-enumeration via timing differences.
 */
const loginUser = async ({ username, password, ipAddress }) => {
  const user = await User.findOne({ where: { username } });

  // Use a dummy hash when user is not found to normalize timing
  const hashToCompare = user
    ? user.passwordHash
    : '$2a$12$invalidhashfortimingpurposes0000000000000000000000000';

  const isValid = await bcrypt.compare(password, hashToCompare);

  if (!user || !isValid) {
    logAction({
      actorId: null,
      action: AUDIT_ACTIONS.LOGIN_FAILURE,
      resourceType: 'User',
      resourceId: null,
      metadata: { username },
      ipAddress,
    });
    const err = new Error('Invalid username or password');
    err.status = 401;
    throw err;
  }

  logAction({
    actorId: user.id,
    action: AUDIT_ACTIONS.LOGIN_SUCCESS,
    resourceType: 'User',
    resourceId: user.id,
    metadata: { username: user.username },
    ipAddress,
  });

  return user;
};

/**
 * Initiate a password reset. Generates a UUID token, stores its bcrypt hash
 * in the DB, and returns the raw token for emailing. Always responds identically
 * whether or not the email is registered to prevent enumeration.
 */
const requestPasswordReset = async ({ email, ipAddress }) => {
  const user = await User.findOne({ where: { email } });

  if (!user) {
    logger.info('Password reset requested for unregistered email', { ipAddress });
    return null;
  }

  const rawToken = uuidv4();
  const tokenHash = await bcrypt.hash(rawToken, 12);
  const expiresAt = new Date(Date.now() + TOKEN_EXPIRY_MS);

  await user.update({ resetToken: tokenHash, resetTokenExpiresAt: expiresAt });

  logAction({
    actorId: user.id,
    action: AUDIT_ACTIONS.PASSWORD_RESET_REQUEST,
    resourceType: 'User',
    resourceId: user.id,
    metadata: {},
    ipAddress,
  });

  return { rawToken, user };
};

/**
 * Complete a password reset. Finds all users with non-expired tokens and
 * bcrypt-compares to find the matching user. Clears token after use.
 */
const resetPassword = async ({ token, newPassword, ipAddress }) => {
  const candidates = await User.findAll({
    where: {
      resetToken: { [Op.ne]: null },
      resetTokenExpiresAt: { [Op.gt]: new Date() },
    },
  });

  let matchedUser = null;
  for (const candidate of candidates) {
    const matches = await bcrypt.compare(token, candidate.resetToken);
    if (matches) {
      matchedUser = candidate;
      break;
    }
  }

  if (!matchedUser) {
    const err = new Error('Invalid or expired password reset token');
    err.status = 400;
    throw err;
  }

  const passwordHash = await User.hashPassword(newPassword);
  await matchedUser.update({ passwordHash, resetToken: null, resetTokenExpiresAt: null });

  logAction({
    actorId: matchedUser.id,
    action: AUDIT_ACTIONS.PASSWORD_RESET_COMPLETE,
    resourceType: 'User',
    resourceId: matchedUser.id,
    metadata: {},
    ipAddress,
  });

  return matchedUser;
};

module.exports = { registerUser, loginUser, requestPasswordReset, resetPassword };
