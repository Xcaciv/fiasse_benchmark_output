'use strict';

const express   = require('express');
const bcrypt    = require('bcryptjs');
const { body, validationResult } = require('express-validator');
const { v4: uuidv4 } = require('uuid');

const db      = require('../config/db');
const logger  = require('../config/logger');
const { sendPasswordResetEmail } = require('../config/mailer');

const router = express.Router();

const SALT_ROUNDS = 12;

// ── Helper: log activity ───────────────────────────────────────────────────
function logActivity(userId, action, details, ip) {
  try {
    db.prepare(`INSERT INTO activity_log (user_id, action, details, ip_address)
                VALUES (?, ?, ?, ?)`).run(userId || null, action, details || null, ip || null);
  } catch (_) { /* non-fatal */ }
}

// ─────────────────────────────────────────────────────────────────────────────
// GET /auth/login
// ─────────────────────────────────────────────────────────────────────────────
router.get('/login', (req, res) => {
  if (req.session.userId) return res.redirect('/notes');
  res.render('auth/login', { title: 'Sign In', errors: [], old: {} });
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /auth/login
// ─────────────────────────────────────────────────────────────────────────────
router.post('/login', [
  body('username').trim().notEmpty().withMessage('Username is required'),
  body('password').notEmpty().withMessage('Password is required')
], async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('auth/login', {
      title: 'Sign In',
      errors: errors.array(),
      old: { username: req.body.username }
    });
  }

  const { username, password } = req.body;

  try {
    const user = db.prepare('SELECT * FROM users WHERE username = ?').get(username.trim());

    if (!user || !(await bcrypt.compare(password, user.password_hash))) {
      // Generic message — don't reveal which field was wrong
      logActivity(null, 'login_failed', `username=${username}`, req.ip);
      logger.warn('Failed login attempt', { username, ip: req.ip });
      return res.render('auth/login', {
        title: 'Sign In',
        errors: [{ msg: 'Invalid username or password.' }],
        old: { username }
      });
    }

    // Regenerate session to prevent session fixation
    req.session.regenerate((err) => {
      if (err) throw err;
      req.session.userId   = user.id;
      req.session.username = user.username;
      req.session.userRole = user.role;

      logActivity(user.id, 'login', null, req.ip);
      logger.info('User logged in', { userId: user.id, username: user.username, ip: req.ip });

      const returnTo = req.session.returnTo || '/notes';
      delete req.session.returnTo;
      res.redirect(returnTo);
    });
  } catch (err) {
    logger.error('Login error', { err: err.message });
    res.render('auth/login', {
      title: 'Sign In',
      errors: [{ msg: 'An error occurred. Please try again.' }],
      old: { username }
    });
  }
});

// ─────────────────────────────────────────────────────────────────────────────
// GET /auth/register
// ─────────────────────────────────────────────────────────────────────────────
router.get('/register', (req, res) => {
  if (req.session.userId) return res.redirect('/notes');
  res.render('auth/register', { title: 'Create Account', errors: [], old: {} });
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /auth/register
// ─────────────────────────────────────────────────────────────────────────────
router.post('/register', [
  body('username')
    .trim()
    .isLength({ min: 3, max: 30 }).withMessage('Username must be 3–30 characters')
    .matches(/^[a-zA-Z0-9_]+$/).withMessage('Username may only contain letters, numbers, and underscores'),
  body('email')
    .trim()
    .isEmail().withMessage('Valid email address is required')
    .normalizeEmail(),
  body('password')
    .isLength({ min: 8 }).withMessage('Password must be at least 8 characters')
    .matches(/[A-Z]/).withMessage('Password must contain at least one uppercase letter')
    .matches(/[0-9]/).withMessage('Password must contain at least one number'),
  body('confirmPassword').custom((value, { req: r }) => {
    if (value !== r.body.password) throw new Error('Passwords do not match');
    return true;
  })
], async (req, res) => {
  const errors = validationResult(req);
  const old = { username: req.body.username, email: req.body.email };

  if (!errors.isEmpty()) {
    return res.render('auth/register', { title: 'Create Account', errors: errors.array(), old });
  }

  const { username, email, password } = req.body;

  try {
    // Check uniqueness
    const existing = db.prepare('SELECT id FROM users WHERE username = ? OR email = ?')
                       .get(username.trim(), email.trim());
    if (existing) {
      return res.render('auth/register', {
        title: 'Create Account',
        errors: [{ msg: 'Username or email is already taken.' }],
        old
      });
    }

    const passwordHash = await bcrypt.hash(password, SALT_ROUNDS);
    const result = db.prepare(`INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)`)
                     .run(username.trim(), email.trim(), passwordHash);

    logActivity(result.lastInsertRowid, 'register', null, req.ip);
    logger.info('New user registered', { userId: result.lastInsertRowid, username });

    req.session.flash = { success: 'Account created! Please sign in.' };
    res.redirect('/auth/login');
  } catch (err) {
    logger.error('Registration error', { err: err.message });
    res.render('auth/register', {
      title: 'Create Account',
      errors: [{ msg: 'An error occurred. Please try again.' }],
      old
    });
  }
});

// ─────────────────────────────────────────────────────────────────────────────
// GET /auth/logout
// ─────────────────────────────────────────────────────────────────────────────
router.get('/logout', (req, res) => {
  const userId   = req.session.userId;
  const username = req.session.username;

  req.session.destroy((err) => {
    if (err) logger.error('Session destroy error', { err: err.message });
    logActivity(userId, 'logout', null, req.ip);
    logger.info('User logged out', { userId, username, ip: req.ip });
    res.clearCookie('lnSid');
    res.redirect('/auth/login');
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// GET /auth/forgot-password
// ─────────────────────────────────────────────────────────────────────────────
router.get('/forgot-password', (req, res) => {
  res.render('auth/forgot-password', { title: 'Reset Password', errors: [], sent: false });
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /auth/forgot-password
// ─────────────────────────────────────────────────────────────────────────────
router.post('/forgot-password', [
  body('email').trim().isEmail().withMessage('Valid email address is required').normalizeEmail()
], async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('auth/forgot-password', { title: 'Reset Password', errors: errors.array(), sent: false });
  }

  // Always render success to avoid email enumeration
  const successView = () =>
    res.render('auth/forgot-password', { title: 'Reset Password', errors: [], sent: true });

  try {
    const user = db.prepare('SELECT id, email FROM users WHERE email = ?').get(req.body.email.trim());
    if (!user) return successView();

    const token   = uuidv4();
    const expires = new Date(Date.now() + 60 * 60 * 1000).toISOString(); // 1 hour

    db.prepare('UPDATE users SET reset_token = ?, reset_token_expires = ? WHERE id = ?')
      .run(token, expires, user.id);

    await sendPasswordResetEmail(user.email, token);
    logActivity(user.id, 'password_reset_requested', null, req.ip);

    successView();
  } catch (err) {
    logger.error('Forgot password error', { err: err.message });
    successView(); // still don't reveal internal errors
  }
});

// ─────────────────────────────────────────────────────────────────────────────
// GET /auth/reset-password/:token
// ─────────────────────────────────────────────────────────────────────────────
router.get('/reset-password/:token', (req, res) => {
  const { token } = req.params;
  const user = db.prepare(
    `SELECT id FROM users WHERE reset_token = ? AND reset_token_expires > datetime('now')`
  ).get(token);

  if (!user) {
    return res.render('auth/reset-password', {
      title: 'Reset Password',
      valid: false,
      token: null,
      errors: []
    });
  }

  res.render('auth/reset-password', { title: 'Reset Password', valid: true, token, errors: [] });
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /auth/reset-password/:token
// ─────────────────────────────────────────────────────────────────────────────
router.post('/reset-password/:token', [
  body('password')
    .isLength({ min: 8 }).withMessage('Password must be at least 8 characters')
    .matches(/[A-Z]/).withMessage('Password must contain at least one uppercase letter')
    .matches(/[0-9]/).withMessage('Password must contain at least one number'),
  body('confirmPassword').custom((value, { req: r }) => {
    if (value !== r.body.password) throw new Error('Passwords do not match');
    return true;
  })
], async (req, res) => {
  const { token } = req.params;
  const errors = validationResult(req);

  const user = db.prepare(
    `SELECT id FROM users WHERE reset_token = ? AND reset_token_expires > datetime('now')`
  ).get(token);

  if (!user) {
    return res.render('auth/reset-password', {
      title: 'Reset Password',
      valid: false, token: null, errors: []
    });
  }

  if (!errors.isEmpty()) {
    return res.render('auth/reset-password', {
      title: 'Reset Password',
      valid: true, token, errors: errors.array()
    });
  }

  try {
    const passwordHash = await bcrypt.hash(req.body.password, SALT_ROUNDS);
    db.prepare('UPDATE users SET password_hash = ?, reset_token = NULL, reset_token_expires = NULL WHERE id = ?')
      .run(passwordHash, user.id);

    logActivity(user.id, 'password_reset_completed', null, req.ip);
    logger.info('Password reset completed', { userId: user.id, ip: req.ip });

    req.session.flash = { success: 'Password updated. Please sign in.' };
    res.redirect('/auth/login');
  } catch (err) {
    logger.error('Password reset error', { err: err.message });
    res.render('auth/reset-password', {
      title: 'Reset Password',
      valid: true, token,
      errors: [{ msg: 'An error occurred. Please try again.' }]
    });
  }
});

module.exports = router;
