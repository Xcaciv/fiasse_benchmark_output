'use strict';
const authService = require('../services/authService');
const { createAuditService } = require('../services/auditService');
const { createEmailService } = require('../services/emailService');
const { validationResult } = require('express-validator');
const logger = require('../utils/logger');
const { User } = require('../models');

const audit = createAuditService(logger);
const emailService = createEmailService(logger);

async function register(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('auth/register', { errors: errors.array(), csrfToken: req.csrfToken() });
  }
  const { username, email, password } = req.body;
  const result = await authService.registerUser({ username, email, password }, { User });
  if (!result.success) {
    return res.render('auth/register', { errors: [{ msg: result.error }], csrfToken: req.csrfToken() });
  }
  audit.logAuthEvent('REGISTER', result.user.id, result.user.username, req.ip, true, {});
  req.flash('success', 'Account created. Please log in.');
  return res.redirect('/auth/login');
}

async function logout(req, res, next) {
  const userId = req.user ? req.user.id : null;
  req.logout((err) => {
    if (err) return next(err);
    audit.logAuthEvent('LOGOUT', userId, null, req.ip, true, {});
    req.flash('success', 'Logged out successfully.');
    return res.redirect('/auth/login');
  });
}

async function forgotPassword(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('auth/forgot-password', { errors: errors.array(), csrfToken: req.csrfToken() });
  }
  const { email } = req.body;
  const baseUrl = process.env.BASE_URL || `${req.protocol}://${req.get('host')}`;
  await authService.requestPasswordReset(email, { User }, emailService, baseUrl);
  // Confidentiality: Always show success to prevent email enumeration
  req.flash('success', 'If that email is registered, a reset link has been sent.');
  return res.redirect('/auth/forgot-password');
}

async function showResetForm(req, res, next) {
  const { token } = req.params;
  const { valid } = await authService.validatePasswordResetToken(token, { User });
  if (!valid) {
    req.flash('error', 'Invalid or expired reset link.');
    return res.redirect('/auth/forgot-password');
  }
  return res.render('auth/reset-password', { token, errors: [], csrfToken: req.csrfToken() });
}

async function resetPassword(req, res, next) {
  const errors = validationResult(req);
  const { token } = req.params;
  if (!errors.isEmpty()) {
    return res.render('auth/reset-password', { token, errors: errors.array(), csrfToken: req.csrfToken() });
  }
  const { password } = req.body;
  const result = await authService.resetPassword(token, password, { User });
  if (!result.success) {
    req.flash('error', result.error);
    return res.redirect('/auth/forgot-password');
  }
  audit.logAuthEvent('PASSWORD_RESET', null, null, req.ip, true, {});
  req.flash('success', 'Password reset. Please log in.');
  return res.redirect('/auth/login');
}

module.exports = { register, logout, forgotPassword, showResetForm, resetPassword };
