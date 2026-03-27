'use strict';

const express = require('express');
const nodemailer = require('nodemailer');
const { registerUser, loginUser, requestPasswordReset, resetPassword } = require('../services/authService');
const {
  registerValidation,
  loginValidation,
  passwordResetValidation,
  forgotPasswordValidation,
} = require('../middleware/validate');
const { authLimiter } = require('../middleware/rateLimiter');
const { logAction, AUDIT_ACTIONS } = require('../services/auditService');
const { logger } = require('../config/logger');

const router = express.Router();

/**
 * Send a password reset email. Errors are logged but do not surface to the user
 * to avoid leaking infrastructure details.
 */
const sendResetEmail = async ({ to, rawToken, username }) => {
  const appUrl = process.env.APP_URL || 'http://localhost:3000';
  const resetUrl = `${appUrl}/auth/reset-password/${rawToken}`;

  const transporter = nodemailer.createTransport({
    host: process.env.SMTP_HOST,
    port: parseInt(process.env.SMTP_PORT || '587', 10),
    secure: process.env.SMTP_PORT === '465',
    auth: {
      user: process.env.SMTP_USER,
      pass: process.env.SMTP_PASS,
    },
  });

  await transporter.sendMail({
    from: process.env.SMTP_FROM || 'noreply@example.com',
    to,
    subject: 'Password Reset – Loose Notes',
    text: `Hello ${username},\n\nTo reset your password visit:\n${resetUrl}\n\nThis link expires in 1 hour.\n\nIf you did not request this, ignore this email.`,
    html: `<p>Hello <strong>${username}</strong>,</p><p>Reset your password: <a href="${resetUrl}">${resetUrl}</a></p><p>This link expires in <strong>1 hour</strong>.</p><p>If you did not request this, please ignore this email.</p>`,
  });
};

// GET /auth/register
router.get('/register', (req, res) => {
  res.render('auth/register', { title: 'Create Account' });
});

// POST /auth/register
router.post('/register', registerValidation, async (req, res, next) => {
  try {
    const { username, email, password } = req.body;
    const user = await registerUser({ username, email, password, ipAddress: req.ip });

    // Regenerate session after privilege change (session fixation prevention)
    req.session.regenerate((err) => {
      if (err) return next(err);
      req.session.userId = user.id;
      req.session.role = user.role;
      req.session.flash = { type: 'success', message: 'Account created! Welcome to Loose Notes.' };
      res.redirect('/notes');
    });
  } catch (err) {
    if (err.name === 'SequelizeUniqueConstraintError') {
      req.session.flash = { type: 'error', message: 'Username or email is already taken.' };
      return res.redirect('/auth/register');
    }
    next(err);
  }
});

// GET /auth/login
router.get('/login', (req, res) => {
  res.render('auth/login', { title: 'Sign In' });
});

// POST /auth/login
router.post('/login', authLimiter, loginValidation, async (req, res, next) => {
  try {
    const { username, password } = req.body;
    const user = await loginUser({ username, password, ipAddress: req.ip });

    req.session.regenerate((err) => {
      if (err) return next(err);
      req.session.userId = user.id;
      req.session.role = user.role;
      req.session.flash = { type: 'success', message: `Welcome back, ${user.username}!` };
      const returnTo = req.session.returnTo || '/notes';
      delete req.session.returnTo;
      res.redirect(returnTo);
    });
  } catch (err) {
    if (err.status === 401) {
      req.session.flash = { type: 'error', message: 'Invalid username or password.' };
      return res.redirect('/auth/login');
    }
    next(err);
  }
});

// POST /auth/logout
router.post('/logout', (req, res, next) => {
  const actorId = req.session.userId;
  const ip = req.ip;

  req.session.destroy((err) => {
    if (err) return next(err);

    logAction({
      actorId,
      action: AUDIT_ACTIONS.LOGOUT,
      resourceType: 'User',
      resourceId: actorId,
      metadata: {},
      ipAddress: ip,
    });

    res.clearCookie('loose_notes_sid');
    res.redirect('/auth/login');
  });
});

// GET /auth/forgot-password
router.get('/forgot-password', (req, res) => {
  res.render('auth/forgot-password', { title: 'Reset Password' });
});

// POST /auth/forgot-password
router.post('/forgot-password', forgotPasswordValidation, async (req, res, next) => {
  try {
    const { email } = req.body;
    const result = await requestPasswordReset({ email, ipAddress: req.ip });

    if (result) {
      sendResetEmail({ to: result.user.email, rawToken: result.rawToken, username: result.user.username })
        .catch((emailErr) => {
          logger.error('Failed to send reset email', { error: emailErr.message });
        });
    }

    // Always show the same message to prevent email enumeration
    req.session.flash = {
      type: 'info',
      message: 'If that email is registered, a reset link has been sent.',
    };
    res.redirect('/auth/login');
  } catch (err) {
    next(err);
  }
});

// GET /auth/reset-password/:token
router.get('/reset-password/:token', (req, res) => {
  res.render('auth/reset-password', {
    title: 'Set New Password',
    token: req.params.token,
  });
});

// POST /auth/reset-password/:token
router.post('/reset-password/:token', passwordResetValidation, async (req, res, next) => {
  try {
    const { password } = req.body;
    await resetPassword({ token: req.params.token, newPassword: password, ipAddress: req.ip });

    req.session.flash = { type: 'success', message: 'Password updated. Please sign in.' };
    res.redirect('/auth/login');
  } catch (err) {
    if (err.status === 400) {
      req.session.flash = { type: 'error', message: 'The reset link is invalid or has expired.' };
      return res.redirect('/auth/forgot-password');
    }
    next(err);
  }
});

module.exports = router;
