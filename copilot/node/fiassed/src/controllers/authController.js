'use strict';
const { v4: uuidv4 } = require('uuid');
const userModel = require('../models/userModel');
const passwordResetModel = require('../models/passwordResetTokenModel');
const passwordService = require('../services/passwordService');
const tokenService = require('../services/tokenService');
const emailService = require('../services/emailService');
const auditService = require('../services/auditService');
const { validateEmail, validateUsername } = require('../utils/validation');
const config = require('../config');

function getRegister(req, res) {
  res.render('auth/register', { error: null });
}

async function postRegister(req, res, db) {
  const { username, email, password } = req.body;
  const ip = req.ip;

  const usernameCheck = validateUsername(username);
  if (!usernameCheck.valid) {
    return res.render('auth/register', { error: usernameCheck.reason });
  }
  const emailCheck = validateEmail(email);
  if (!emailCheck.valid) {
    return res.render('auth/register', { error: emailCheck.reason });
  }
  const passCheck = passwordService.validatePasswordPolicy(password);
  if (!passCheck.valid) {
    return res.render('auth/register', { error: passCheck.reason });
  }

  try {
    const existingUser = userModel.findByUsername(db, username) || userModel.findByEmail(db, email);
    if (existingUser) {
      return res.render('auth/register', { error: 'Registration failed. Please try different credentials.' });
    }

    const passwordHash = await passwordService.hashPassword(password);
    const userId = uuidv4();
    userModel.createUser(db, { id: userId, username, email, passwordHash });

    auditService.log({ eventType: 'USER_REGISTERED', userId, ipAddress: ip, resourceType: 'user', resourceId: userId });
    req.session.flash = { success: 'Registration successful. Please log in.' };
    res.redirect('/login');
  } catch (err) {
    res.render('auth/register', { error: 'Registration failed. Please try again.' });
  }
}

function getLogin(req, res) {
  res.render('auth/login', { error: null });
}

async function postLogin(req, res, db) {
  const { username, password } = req.body;
  const ip = req.ip;

  if (!username || !password) {
    return res.render('auth/login', { error: 'Invalid credentials' });
  }

  const user = userModel.findByUsername(db, username);

  if (!user) {
    auditService.log({ eventType: 'LOGIN_FAILURE', ipAddress: ip, details: { reason: 'user_not_found' } });
    return res.render('auth/login', { error: 'Invalid credentials' });
  }

  const valid = await passwordService.verifyPassword(password, user.password_hash);
  if (!valid) {
    auditService.log({ eventType: 'LOGIN_FAILURE', ipAddress: ip, userId: user.id, details: { reason: 'bad_password' } });
    return res.render('auth/login', { error: 'Invalid credentials' });
  }

  // Prevent session fixation by regenerating session before setting data
  req.session.regenerate((err) => {
    if (err) return res.render('auth/login', { error: 'Login failed. Please try again.' });
    req.session.userId = user.id;
    req.session.username = user.username;
    req.session.role = user.role;
    auditService.log({ eventType: 'LOGIN_SUCCESS', userId: user.id, ipAddress: ip });
    res.redirect('/notes');
  });
}

function postLogout(req, res) {
  const userId = req.session ? req.session.userId : null;
  req.session.destroy(() => {
    auditService.log({ eventType: 'LOGOUT', userId });
    res.redirect('/login');
  });
}

function getForgotPassword(req, res) {
  res.render('auth/forgot-password', { message: null, error: null });
}

async function postForgotPassword(req, res, db) {
  const { email } = req.body;
  const successMessage = 'If an account exists with that email, a reset link has been sent.';

  const emailCheck = validateEmail(email);
  if (!emailCheck.valid) {
    return res.render('auth/forgot-password', { message: successMessage, error: null });
  }

  try {
    const user = userModel.findByEmail(db, email);
    if (user) {
      const token = tokenService.generateSecureToken();
      const tokenHash = tokenService.hashToken(token);
      const expiresAt = Date.now() + config.resetTokenExpiryMs;

      db.prepare(
        'INSERT INTO password_reset_tokens (id, user_id, token_hash, expires_at, used) VALUES (?, ?, ?, ?, 0)'
      ).run(uuidv4(), user.id, tokenHash, expiresAt);

      emailService.sendPasswordResetEmail(user.email, token);
      auditService.log({ eventType: 'PASSWORD_RESET_REQUESTED', userId: user.id, ipAddress: req.ip });
    }
  } catch (err) {
    // Swallow errors to prevent user enumeration
  }

  res.render('auth/forgot-password', { message: successMessage, error: null });
}

function getResetPassword(req, res, db) {
  const { token } = req.params;
  const tokenHash = tokenService.hashToken(token);
  const record = db.prepare(
    'SELECT * FROM password_reset_tokens WHERE token_hash = ? AND used = 0 AND expires_at > ?'
  ).get(tokenHash, Date.now());

  if (!record) {
    return res.render('auth/reset-password', { error: 'Invalid or expired reset link.', token: null });
  }
  res.render('auth/reset-password', { error: null, token });
}

async function postResetPassword(req, res, db) {
  const { token } = req.params;
  const { password, confirmPassword } = req.body;

  if (password !== confirmPassword) {
    return res.render('auth/reset-password', { error: 'Passwords do not match.', token });
  }

  const passCheck = passwordService.validatePasswordPolicy(password);
  if (!passCheck.valid) {
    return res.render('auth/reset-password', { error: passCheck.reason, token });
  }

  const tokenHash = tokenService.hashToken(token);
  const record = db.prepare(
    'SELECT * FROM password_reset_tokens WHERE token_hash = ? AND used = 0 AND expires_at > ?'
  ).get(tokenHash, Date.now());

  if (!record) {
    return res.render('auth/reset-password', { error: 'Invalid or expired reset link.', token: null });
  }

  const passwordHash = await passwordService.hashPassword(password);
  userModel.updatePassword(db, record.user_id, passwordHash);
  db.prepare('UPDATE password_reset_tokens SET used = 1 WHERE id = ?').run(record.id);

  auditService.log({ eventType: 'PASSWORD_RESET_COMPLETED', userId: record.user_id, ipAddress: req.ip });
  req.session.flash = { success: 'Password reset successfully. Please log in.' };
  res.redirect('/login');
}

module.exports = { getRegister, postRegister, getLogin, postLogin, postLogout, getForgotPassword, postForgotPassword, getResetPassword, postResetPassword };
