'use strict';

const express = require('express');
const passport = require('passport');
const bcrypt = require('bcrypt');
const crypto = require('crypto');
const { body } = require('express-validator');
const { User } = require('../models');
const { requireGuest } = require('../middleware/auth');
const { authLimiter } = require('../middleware/rateLimiter');
const { handleValidation } = require('../middleware/validate');
const audit = require('../services/auditService');
const email = require('../services/emailService');
const security = require('../config/security');

const router = express.Router();

// ── Registration ────────────────────────────────────────────────────────────

const registerRules = [
  body('username')
    .trim()
    .isLength({ min: security.USERNAME_MIN_LENGTH, max: security.USERNAME_MAX_LENGTH })
    .withMessage(`Username must be ${security.USERNAME_MIN_LENGTH}–${security.USERNAME_MAX_LENGTH} characters`)
    .matches(/^[a-zA-Z0-9_-]+$/)
    .withMessage('Username may only contain letters, numbers, underscores, and hyphens'),
  body('email')
    .trim()
    .isEmail()
    .withMessage('Valid email required')
    .normalizeEmail(),
  body('password')
    .isLength({ min: security.PASSWORD_MIN_LENGTH, max: security.PASSWORD_MAX_LENGTH })
    .withMessage(`Password must be ${security.PASSWORD_MIN_LENGTH}–${security.PASSWORD_MAX_LENGTH} characters`),
  body('confirmPassword')
    .custom((val, { req }) => val === req.body.password)
    .withMessage('Passwords do not match'),
];

router.get('/register', requireGuest, (req, res) => {
  res.render('auth/register', { title: 'Register', csrfToken: req.csrfToken() });
});

router.post('/register', requireGuest, authLimiter, registerRules, handleValidation,
  async (req, res, next) => {
    try {
      const { username, email: rawEmail, password } = req.body;
      const normalizedEmail = rawEmail.trim().toLowerCase();
      const normalizedUsername = username.trim().toLowerCase();

      const existing = await User.findOne({
        where: { username: normalizedUsername },
      });
      if (existing) {
        req.flash('error', 'Username already taken.');
        return res.redirect('/auth/register');
      }

      const existingEmail = await User.findOne({ where: { email: normalizedEmail } });
      if (existingEmail) {
        req.flash('error', 'Email already registered.');
        return res.redirect('/auth/register');
      }

      const passwordHash = await bcrypt.hash(password, security.BCRYPT_ROUNDS);
      const user = await User.create({ username: normalizedUsername, email: normalizedEmail, passwordHash });

      await audit.record('user.register', user.id, { username: normalizedUsername }, req.ip);
      req.flash('success', 'Account created. Please log in.');
      return res.redirect('/auth/login');
    } catch (err) {
      return next(err);
    }
  }
);

// ── Login ────────────────────────────────────────────────────────────────────

router.get('/login', requireGuest, (req, res) => {
  res.render('auth/login', { title: 'Login', csrfToken: req.csrfToken() });
});

router.post('/login', requireGuest, authLimiter,
  (req, res, next) => {
    passport.authenticate('local', async (err, user, info) => {
      if (err) return next(err);

      if (!user) {
        await audit.record('auth.login.failed', null, { username: req.body.username }, req.ip);
        req.flash('error', info?.message || 'Invalid credentials.');
        return res.redirect('/auth/login');
      }

      req.logIn(user, async (loginErr) => {
        if (loginErr) return next(loginErr);
        await audit.record('auth.login', user.id, {}, req.ip);
        return res.redirect('/notes');
      });
    })(req, res, next);
  }
);

// ── Logout ───────────────────────────────────────────────────────────────────

router.post('/logout', (req, res, next) => {
  const userId = req.user?.id;
  req.logout((err) => {
    if (err) return next(err);
    audit.record('auth.logout', userId, {}, req.ip);
    req.session.destroy(() => {
      res.clearCookie('connect.sid');
      res.redirect('/auth/login');
    });
  });
});

// ── Forgot Password ───────────────────────────────────────────────────────────

router.get('/forgot-password', requireGuest, (req, res) => {
  res.render('auth/forgot-password', { title: 'Forgot Password', csrfToken: req.csrfToken() });
});

router.post('/forgot-password', requireGuest, authLimiter,
  body('email').trim().isEmail().normalizeEmail().withMessage('Valid email required'),
  handleValidation,
  async (req, res, next) => {
    try {
      const normalizedEmail = req.body.email.trim().toLowerCase();
      const user = await User.findOne({ where: { email: normalizedEmail } });

      // Always show same response to prevent user enumeration
      if (user) {
        const rawToken = crypto.randomBytes(32).toString('hex');
        const tokenHash = crypto.createHash('sha256').update(rawToken).digest('hex');
        const expiry = new Date(Date.now() + security.RESET_TOKEN_TTL_MS);

        await user.update({ resetTokenHash: tokenHash, resetTokenExpiry: expiry });

        const resetUrl = `${req.protocol}://${req.get('host')}/auth/reset-password?token=${rawToken}&email=${encodeURIComponent(normalizedEmail)}`;
        await email.sendPasswordResetEmail(normalizedEmail, resetUrl);
        await audit.record('auth.password_reset_requested', user.id, {}, req.ip);
      }

      req.flash('success', 'If that email is registered, a reset link has been sent.');
      return res.redirect('/auth/forgot-password');
    } catch (err) {
      return next(err);
    }
  }
);

// ── Reset Password ────────────────────────────────────────────────────────────

router.get('/reset-password', requireGuest, (req, res) => {
  res.render('auth/reset-password', {
    title: 'Reset Password',
    token: req.query.token || '',
    emailParam: req.query.email || '',
    csrfToken: req.csrfToken(),
  });
});

const resetRules = [
  body('password')
    .isLength({ min: security.PASSWORD_MIN_LENGTH, max: security.PASSWORD_MAX_LENGTH })
    .withMessage(`Password must be ${security.PASSWORD_MIN_LENGTH}–${security.PASSWORD_MAX_LENGTH} characters`),
  body('confirmPassword')
    .custom((val, { req: r }) => val === r.body.password)
    .withMessage('Passwords do not match'),
];

router.post('/reset-password', requireGuest, authLimiter, resetRules, handleValidation,
  async (req, res, next) => {
    try {
      const { token, email: rawEmail, password } = req.body;
      const normalizedEmail = rawEmail.trim().toLowerCase();

      const user = await User.findOne({ where: { email: normalizedEmail } });

      if (!user || !user.resetTokenHash || !user.resetTokenExpiry) {
        req.flash('error', 'Invalid or expired reset link.');
        return res.redirect('/auth/forgot-password');
      }

      if (new Date() > user.resetTokenExpiry) {
        req.flash('error', 'Reset link has expired. Please request a new one.');
        return res.redirect('/auth/forgot-password');
      }

      const tokenHash = crypto.createHash('sha256').update(token).digest('hex');
      // Constant-time comparison to prevent timing attacks
      const tokenValid = crypto.timingSafeEqual(
        Buffer.from(tokenHash),
        Buffer.from(user.resetTokenHash)
      );

      if (!tokenValid) {
        req.flash('error', 'Invalid or expired reset link.');
        return res.redirect('/auth/forgot-password');
      }

      const passwordHash = await bcrypt.hash(password, security.BCRYPT_ROUNDS);
      await user.update({ passwordHash, resetTokenHash: null, resetTokenExpiry: null });
      await audit.record('auth.password_reset', user.id, {}, req.ip);

      req.flash('success', 'Password updated. Please log in with your new password.');
      return res.redirect('/auth/login');
    } catch (err) {
      return next(err);
    }
  }
);

module.exports = router;
