'use strict';

const express = require('express');
const bcrypt  = require('bcryptjs');
const { body, validationResult } = require('express-validator');

const db     = require('../config/db');
const logger = require('../config/logger');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();
router.use(requireAuth);

const SALT_ROUNDS = 12;

// ─────────────────────────────────────────────────────────────────────────────
// GET /profile
// ─────────────────────────────────────────────────────────────────────────────
router.get('/', (req, res) => {
  const user = db.prepare('SELECT id, username, email, role, created_at FROM users WHERE id = ?')
                 .get(req.session.userId);
  if (!user) return res.redirect('/auth/logout');
  res.render('profile/edit', { title: 'My Profile', user, errors: [], section: 'info' });
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /profile/info — update username / email
// ─────────────────────────────────────────────────────────────────────────────
router.post('/info', [
  body('username')
    .trim()
    .isLength({ min: 3, max: 30 }).withMessage('Username must be 3–30 characters')
    .matches(/^[a-zA-Z0-9_]+$/).withMessage('Username may only contain letters, numbers, and underscores'),
  body('email')
    .trim()
    .isEmail().withMessage('Valid email address is required')
    .normalizeEmail()
], (req, res) => {
  const user = db.prepare('SELECT id, username, email, role, created_at FROM users WHERE id = ?')
                 .get(req.session.userId);

  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('profile/edit', { title: 'My Profile', user, errors: errors.array(), section: 'info' });
  }

  const { username, email } = req.body;

  // Check uniqueness (excluding self)
  const conflict = db.prepare(
    'SELECT id FROM users WHERE (username = ? OR email = ?) AND id != ?'
  ).get(username.trim(), email.trim(), req.session.userId);

  if (conflict) {
    return res.render('profile/edit', {
      title: 'My Profile', user,
      errors: [{ msg: 'Username or email is already taken.' }],
      section: 'info'
    });
  }

  db.prepare('UPDATE users SET username = ?, email = ? WHERE id = ?')
    .run(username.trim(), email.trim(), req.session.userId);

  // Keep session in sync
  req.session.username = username.trim();

  logger.info('Profile updated', { userId: req.session.userId });
  req.session.flash = { success: 'Profile updated.' };
  res.redirect('/profile');
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /profile/password — change password
// ─────────────────────────────────────────────────────────────────────────────
router.post('/password', [
  body('current_password').notEmpty().withMessage('Current password is required'),
  body('password')
    .isLength({ min: 8 }).withMessage('New password must be at least 8 characters')
    .matches(/[A-Z]/).withMessage('New password must contain at least one uppercase letter')
    .matches(/[0-9]/).withMessage('New password must contain at least one number'),
  body('confirmPassword').custom((value, { req: r }) => {
    if (value !== r.body.password) throw new Error('Passwords do not match');
    return true;
  })
], async (req, res) => {
  const user = db.prepare('SELECT * FROM users WHERE id = ?').get(req.session.userId);

  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    const safeUser = { id: user.id, username: user.username, email: user.email, role: user.role, created_at: user.created_at };
    return res.render('profile/edit', { title: 'My Profile', user: safeUser, errors: errors.array(), section: 'password' });
  }

  const valid = await bcrypt.compare(req.body.current_password, user.password_hash);
  if (!valid) {
    const safeUser = { id: user.id, username: user.username, email: user.email, role: user.role, created_at: user.created_at };
    return res.render('profile/edit', {
      title: 'My Profile', user: safeUser,
      errors: [{ msg: 'Current password is incorrect.' }],
      section: 'password'
    });
  }

  const newHash = await bcrypt.hash(req.body.password, SALT_ROUNDS);
  db.prepare('UPDATE users SET password_hash = ? WHERE id = ?').run(newHash, req.session.userId);

  logger.info('Password changed', { userId: req.session.userId });
  req.session.flash = { success: 'Password changed successfully.' };
  res.redirect('/profile');
});

module.exports = router;
