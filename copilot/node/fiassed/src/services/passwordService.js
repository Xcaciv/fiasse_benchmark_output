'use strict';
const bcrypt = require('bcrypt');
const config = require('../config');
const COMMON_PASSWORDS = require('../utils/commonPasswords');

const BCRYPT_ROUNDS = 12;

async function hashPassword(password) {
  return bcrypt.hash(password, BCRYPT_ROUNDS);
}

async function verifyPassword(password, hash) {
  return bcrypt.compare(password, hash);
}

function validatePasswordPolicy(password) {
  if (!password || typeof password !== 'string') {
    return { valid: false, reason: 'Password is required' };
  }
  if (password.length < config.passwordMinLength) {
    return { valid: false, reason: `Password must be at least ${config.passwordMinLength} characters` };
  }
  if (password.length > config.passwordMaxLength) {
    return { valid: false, reason: 'Password exceeds maximum length' };
  }
  if (COMMON_PASSWORDS.has(password.toLowerCase())) {
    return { valid: false, reason: 'Password is too common. Please choose a stronger password' };
  }
  return { valid: true };
}

module.exports = { hashPassword, verifyPassword, validatePasswordPolicy };
