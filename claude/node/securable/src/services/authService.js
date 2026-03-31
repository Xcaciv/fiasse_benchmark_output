'use strict';

const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const { User, PasswordResetToken } = require('../models');
const security = require('../config/security');
const emailService = require('./emailService');
const auditService = require('./auditService');
const logger = require('../config/logger');

/**
 * Auth service — account creation, password management, reset flow.
 * SSEM Authenticity / Confidentiality: passwords hashed with bcrypt (cost 12),
 * reset tokens stored as bcrypt hashes — raw value sent by email only.
 */

async function registerUser({ username, email, password }) {
  // Canonicalise at service boundary (S6.4.1)
  const canonicalUsername = username.trim().toLowerCase();
  const canonicalEmail = email.trim().toLowerCase();

  const existing = await User.findOne({
    where: { username: canonicalUsername },
    attributes: ['id'],
  });
  if (existing) {
    const err = new Error('Username already taken.');
    err.status = 409;
    throw err;
  }

  const emailTaken = await User.findOne({
    where: { email: canonicalEmail },
    attributes: ['id'],
  });
  if (emailTaken) {
    const err = new Error('Email address already registered.');
    err.status = 409;
    throw err;
  }

  const passwordHash = await bcrypt.hash(password, security.bcryptRounds);
  const role =
    security.adminEmail && canonicalEmail === security.adminEmail ? 'admin' : 'user';

  const user = await User.create({
    username: canonicalUsername,
    email: canonicalEmail,
    passwordHash,
    role,
  });

  logger.audit('REGISTER', { userId: user.id });
  await auditService.record({ userId: user.id, action: 'REGISTER' });

  return user;
}

async function initiatePasswordReset(email, baseUrl) {
  const canonicalEmail = email.trim().toLowerCase();
  // Look up with password hash scope to get userId
  const user = await User.scope('withPassword').findOne({
    where: { email: canonicalEmail },
    attributes: ['id', 'email'],
  });

  // Always respond the same way to prevent email enumeration
  if (!user) {
    logger.info({ event: 'PASSWORD_RESET_REQUEST', found: false });
    return;
  }

  // Invalidate existing tokens for this user
  await PasswordResetToken.update(
    { usedAt: new Date() },
    { where: { userId: user.id, usedAt: null } }
  );

  const rawToken = crypto.randomBytes(48).toString('hex');
  const tokenHash = await bcrypt.hash(rawToken, 10);
  const expiresAt = new Date(Date.now() + security.resetTokenExpiryMs);

  await PasswordResetToken.create({ userId: user.id, tokenHash, expiresAt });

  const resetUrl = `${baseUrl}/auth/reset-password?token=${rawToken}&uid=${user.id}`;
  await emailService.sendPasswordReset(user.email, resetUrl);

  logger.audit('PASSWORD_RESET_INITIATED', { userId: user.id });
}

async function resetPassword(userId, rawToken, newPassword) {
  const record = await PasswordResetToken.findOne({
    where: { userId, usedAt: null },
    order: [['created_at', 'DESC']],
  });

  if (!record) {
    const err = new Error('Invalid or expired reset token.');
    err.status = 400;
    throw err;
  }

  if (record.expiresAt < new Date()) {
    const err = new Error('Password reset token has expired.');
    err.status = 400;
    throw err;
  }

  const valid = await bcrypt.compare(rawToken, record.tokenHash);
  if (!valid) {
    const err = new Error('Invalid or expired reset token.');
    err.status = 400;
    throw err;
  }

  const passwordHash = await bcrypt.hash(newPassword, security.bcryptRounds);
  await User.scope('withPassword').update(
    { passwordHash },
    { where: { id: userId } }
  );

  // Mark token used (no delete — audit trail)
  await record.update({ usedAt: new Date() });

  logger.audit('PASSWORD_RESET_COMPLETE', { userId });
  await auditService.record({ userId, action: 'PASSWORD_RESET' });
}

async function changePassword(userId, currentPassword, newPassword) {
  const user = await User.scope('withPassword').findByPk(userId);
  if (!user) {
    const err = new Error('User not found.');
    err.status = 404;
    throw err;
  }

  const match = await bcrypt.compare(currentPassword, user.passwordHash);
  if (!match) {
    const err = new Error('Current password is incorrect.');
    err.status = 400;
    throw err;
  }

  const passwordHash = await bcrypt.hash(newPassword, security.bcryptRounds);
  await User.scope('withPassword').update({ passwordHash }, { where: { id: userId } });

  logger.audit('PASSWORD_CHANGED', { userId });
  await auditService.record({ userId, action: 'PASSWORD_CHANGED' });
}

module.exports = { registerUser, initiatePasswordReset, resetPassword, changePassword };
