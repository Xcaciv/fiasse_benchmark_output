'use strict';
const bcrypt = require('bcryptjs');
const { BCRYPT_SALT_ROUNDS, PASSWORD_RESET_TOKEN_EXPIRY_MS } = require('../config/constants');
const { generateSecureToken, hashToken } = require('../utils/tokenHelper');
const { canonicalize } = require('../utils/inputHandler');

// Returns { success, error, user } — no throws for expected errors
async function registerUser({ username, email, password }, { User }) {
  const normalizedUsername = canonicalize(username).toLowerCase();
  const normalizedEmail = canonicalize(email).toLowerCase();
  const existing = await User.findOne({
    where: { username: normalizedUsername }
  });
  if (existing) return { success: false, error: 'Username already taken' };
  const emailExists = await User.findOne({ where: { email: normalizedEmail } });
  if (emailExists) return { success: false, error: 'Email already registered' };
  const passwordHash = await bcrypt.hash(password, BCRYPT_SALT_ROUNDS);
  const user = await User.create({
    username: normalizedUsername,
    email: normalizedEmail,
    passwordHash
  });
  return { success: true, user };
}

async function requestPasswordReset(email, { User }, emailService, baseUrl) {
  const normalizedEmail = canonicalize(email).toLowerCase();
  const user = await User.findOne({ where: { email: normalizedEmail } });
  // Confidentiality: Do not reveal whether email exists
  if (!user) return { success: true };
  const token = generateSecureToken(32);
  const tokenHash = hashToken(token);
  const expiry = new Date(Date.now() + PASSWORD_RESET_TOKEN_EXPIRY_MS);
  await user.update({ passwordResetTokenHash: tokenHash, passwordResetExpiry: expiry });
  await emailService.sendPasswordResetEmail(normalizedEmail, token, baseUrl);
  return { success: true };
}

async function validatePasswordResetToken(token, { User }) {
  const tokenHash = hashToken(token);
  const user = await User.findOne({ where: { passwordResetTokenHash: tokenHash } });
  if (!user) return { valid: false };
  if (new Date() > user.passwordResetExpiry) return { valid: false };
  return { valid: true, user };
}

async function resetPassword(token, newPassword, { User }) {
  const { valid, user } = await validatePasswordResetToken(token, { User });
  if (!valid) return { success: false, error: 'Invalid or expired token' };
  const passwordHash = await bcrypt.hash(newPassword, BCRYPT_SALT_ROUNDS);
  await user.update({ passwordHash, passwordResetTokenHash: null, passwordResetExpiry: null });
  return { success: true };
}

module.exports = { registerUser, requestPasswordReset, validatePasswordResetToken, resetPassword };
