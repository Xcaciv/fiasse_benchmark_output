const express = require('express');
const router = express.Router();
const passport = require('passport');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');
const { getDb, logActivity } = require('../database/db');

// GET /auth/login
router.get('/login', (req, res) => {
  if (req.isAuthenticated()) return res.redirect('/notes');
  res.render('auth/login', { title: 'Login' });
});

// POST /auth/login
router.post('/login', (req, res, next) => {
  passport.authenticate('local', (err, user, info) => {
    if (err) return next(err);
    if (!user) {
      logActivity(null, 'login_failed', `Failed login attempt for: ${req.body.username}`);
      req.flash('error', info.message || 'Login failed.');
      return res.redirect('/auth/login');
    }
    req.logIn(user, (err) => {
      if (err) return next(err);
      logActivity(user.id, 'login', `User logged in: ${user.username}`);
      req.flash('success', `Welcome back, ${user.username}!`);
      res.redirect('/notes');
    });
  })(req, res, next);
});

// GET /auth/register
router.get('/register', (req, res) => {
  if (req.isAuthenticated()) return res.redirect('/notes');
  res.render('auth/register', { title: 'Register' });
});

// POST /auth/register
router.post('/register', (req, res) => {
  const { username, email, password, confirmPassword } = req.body;
  const db = getDb();

  if (!username || !email || !password || !confirmPassword) {
    req.flash('error', 'All fields are required.');
    return res.redirect('/auth/register');
  }

  if (password !== confirmPassword) {
    req.flash('error', 'Passwords do not match.');
    return res.redirect('/auth/register');
  }

  if (password.length < 6) {
    req.flash('error', 'Password must be at least 6 characters.');
    return res.redirect('/auth/register');
  }

  const existing = db.prepare(
    'SELECT id FROM users WHERE username = ? OR email = ?'
  ).get(username, email);

  if (existing) {
    req.flash('error', 'Username or email is already in use.');
    return res.redirect('/auth/register');
  }

  const hash = bcrypt.hashSync(password, 10);
  const result = db.prepare(
    'INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)'
  ).run(username, email, hash);

  logActivity(result.lastInsertRowid, 'register', `New user registered: ${username}`);
  req.flash('success', 'Account created! Please log in.');
  res.redirect('/auth/login');
});

// POST /auth/logout
router.post('/logout', (req, res, next) => {
  const username = req.user ? req.user.username : 'unknown';
  req.logout((err) => {
    if (err) return next(err);
    logActivity(null, 'logout', `User logged out: ${username}`);
    req.flash('success', 'You have been logged out.');
    res.redirect('/auth/login');
  });
});

// GET /auth/forgot-password
router.get('/forgot-password', (req, res) => {
  res.render('auth/forgot-password', { title: 'Forgot Password' });
});

// POST /auth/forgot-password
router.post('/forgot-password', (req, res) => {
  const { email } = req.body;
  const db = getDb();

  const user = db.prepare('SELECT * FROM users WHERE email = ?').get(email);

  if (!user) {
    // Don't reveal whether email exists
    req.flash('success', 'If that email is registered, a reset link has been sent.');
    return res.redirect('/auth/forgot-password');
  }

  // Invalidate old tokens
  db.prepare('UPDATE password_resets SET used = 1 WHERE user_id = ?').run(user.id);

  const token = uuidv4();
  const expiresAt = new Date(Date.now() + 60 * 60 * 1000).toISOString(); // 1 hour

  db.prepare(
    'INSERT INTO password_resets (user_id, token, expires_at) VALUES (?, ?, ?)'
  ).run(user.id, token, expiresAt);

  const resetUrl = `${req.protocol}://${req.get('host')}/auth/reset-password/${token}`;
  console.log(`\n[Password Reset] User: ${user.username} | URL: ${resetUrl}\n`);

  req.flash('success', 'A password reset link has been generated. Check the server console for the link (email not configured).');
  res.redirect('/auth/forgot-password');
});

// GET /auth/reset-password/:token
router.get('/reset-password/:token', (req, res) => {
  const db = getDb();
  const reset = db.prepare(
    'SELECT * FROM password_resets WHERE token = ? AND used = 0'
  ).get(req.params.token);

  if (!reset || new Date(reset.expires_at) < new Date()) {
    req.flash('error', 'Reset link is invalid or has expired.');
    return res.redirect('/auth/forgot-password');
  }

  res.render('auth/reset-password', { title: 'Reset Password', token: req.params.token });
});

// POST /auth/reset-password/:token
router.post('/reset-password/:token', (req, res) => {
  const db = getDb();
  const { password, confirmPassword } = req.body;

  const reset = db.prepare(
    'SELECT * FROM password_resets WHERE token = ? AND used = 0'
  ).get(req.params.token);

  if (!reset || new Date(reset.expires_at) < new Date()) {
    req.flash('error', 'Reset link is invalid or has expired.');
    return res.redirect('/auth/forgot-password');
  }

  if (!password || password !== confirmPassword) {
    req.flash('error', 'Passwords do not match.');
    return res.redirect(`/auth/reset-password/${req.params.token}`);
  }

  if (password.length < 6) {
    req.flash('error', 'Password must be at least 6 characters.');
    return res.redirect(`/auth/reset-password/${req.params.token}`);
  }

  const hash = bcrypt.hashSync(password, 10);
  db.prepare('UPDATE users SET password_hash = ? WHERE id = ?').run(hash, reset.user_id);
  db.prepare('UPDATE password_resets SET used = 1 WHERE id = ?').run(reset.id);

  logActivity(reset.user_id, 'password_reset', 'Password was reset via token');
  req.flash('success', 'Password reset successfully. Please log in.');
  res.redirect('/auth/login');
});

module.exports = router;
