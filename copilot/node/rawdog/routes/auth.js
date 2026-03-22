const express = require('express');
const router = express.Router();
const bcrypt = require('bcrypt');
const passport = require('passport');
const { v4: uuidv4 } = require('uuid');
const nodemailer = require('nodemailer');
const { body, validationResult } = require('express-validator');
const { User } = require('../models');
const logger = require('../config/logger');

// GET /auth/register
router.get('/register', (req, res) => {
  res.render('auth/register', { title: 'Register', user: null, errors: [], messages: req.flash() });
});

// POST /auth/register
router.post('/register', [
  body('username').trim().isLength({ min: 3 }).withMessage('Username must be at least 3 characters.'),
  body('email').isEmail().normalizeEmail().withMessage('Enter a valid email.'),
  body('password').isLength({ min: 6 }).withMessage('Password must be at least 6 characters.'),
], async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('auth/register', { title: 'Register', user: null, errors: errors.array(), messages: req.flash() });
  }
  const { username, email, password } = req.body;
  try {
    const existing = await User.findOne({ where: { email } });
    if (existing) {
      return res.render('auth/register', { title: 'Register', user: null, errors: [{ msg: 'Email already registered.' }], messages: req.flash() });
    }
    const usernameExists = await User.findOne({ where: { username } });
    if (usernameExists) {
      return res.render('auth/register', { title: 'Register', user: null, errors: [{ msg: 'Username already taken.' }], messages: req.flash() });
    }
    const passwordHash = await bcrypt.hash(password, 12);
    await User.create({ username, email, passwordHash });
    logger.info(`New user registered: ${email}`);
    req.flash('success', 'Registration successful! Please log in.');
    res.redirect('/auth/login');
  } catch (err) {
    logger.error(`Registration error: ${err.message}`);
    res.render('auth/register', { title: 'Register', user: null, errors: [{ msg: 'An error occurred. Please try again.' }], messages: req.flash() });
  }
});

// GET /auth/login
router.get('/login', (req, res) => {
  res.render('auth/login', { title: 'Login', user: null, errors: [], messages: req.flash() });
});

// POST /auth/login
router.post('/login', (req, res, next) => {
  passport.authenticate('local', (err, user, info) => {
    if (err) return next(err);
    if (!user) {
      req.flash('error', info.message || 'Login failed.');
      return res.redirect('/auth/login');
    }
    req.logIn(user, (err) => {
      if (err) return next(err);
      logger.info(`User logged in: ${user.email}`);
      req.flash('success', `Welcome back, ${user.username}!`);
      res.redirect('/notes');
    });
  })(req, res, next);
});

// GET /auth/logout
router.get('/logout', (req, res, next) => {
  const email = req.user ? req.user.email : 'unknown';
  req.logout((err) => {
    if (err) return next(err);
    logger.info(`User logged out: ${email}`);
    req.flash('success', 'You have been logged out.');
    res.redirect('/auth/login');
  });
});

// GET /auth/forgot
router.get('/forgot', (req, res) => {
  res.render('auth/forgot-password', { title: 'Forgot Password', user: null, errors: [], messages: req.flash() });
});

// POST /auth/forgot
router.post('/forgot', [
  body('email').isEmail().normalizeEmail().withMessage('Enter a valid email.'),
], async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('auth/forgot-password', { title: 'Forgot Password', user: null, errors: errors.array(), messages: req.flash() });
  }
  const { email } = req.body;
  try {
    const user = await User.findOne({ where: { email } });
    if (!user) {
      req.flash('success', 'If that email exists, a reset link has been sent.');
      return res.redirect('/auth/forgot');
    }
    const token = uuidv4();
    const expires = new Date(Date.now() + 3600000); // 1 hour
    await user.update({ resetToken: token, resetExpires: expires });

    const resetUrl = `${process.env.APP_URL || 'http://localhost:3000'}/auth/reset/${token}`;
    console.log(`[PASSWORD RESET] Token for ${email}: ${resetUrl}`);
    logger.info(`Password reset requested for: ${email}`);

    // Send email if SMTP configured
    if (process.env.SMTP_HOST) {
      const transporter = nodemailer.createTransport({
        host: process.env.SMTP_HOST,
        port: parseInt(process.env.SMTP_PORT) || 587,
        auth: { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS },
      });
      await transporter.sendMail({
        from: process.env.SMTP_FROM || 'noreply@loosenotes.local',
        to: email,
        subject: 'Loose Notes – Password Reset',
        text: `Click the link to reset your password (valid 1 hour):\n\n${resetUrl}`,
        html: `<p>Click the link to reset your password (valid 1 hour):</p><p><a href="${resetUrl}">${resetUrl}</a></p>`,
      });
    }

    req.flash('success', 'If that email exists, a reset link has been sent.');
    res.redirect('/auth/forgot');
  } catch (err) {
    logger.error(`Forgot password error: ${err.message}`);
    req.flash('error', 'An error occurred. Please try again.');
    res.redirect('/auth/forgot');
  }
});

// GET /auth/reset/:token
router.get('/reset/:token', async (req, res) => {
  try {
    const user = await User.findOne({
      where: { resetToken: req.params.token, resetExpires: { [require('sequelize').Op.gt]: new Date() } },
    });
    if (!user) {
      req.flash('error', 'Password reset token is invalid or has expired.');
      return res.redirect('/auth/forgot');
    }
    res.render('auth/reset-password', { title: 'Reset Password', user: null, token: req.params.token, errors: [], messages: req.flash() });
  } catch (err) {
    req.flash('error', 'An error occurred.');
    res.redirect('/auth/forgot');
  }
});

// POST /auth/reset/:token
router.post('/reset/:token', [
  body('password').isLength({ min: 6 }).withMessage('Password must be at least 6 characters.'),
  body('confirmPassword').custom((value, { req }) => {
    if (value !== req.body.password) throw new Error('Passwords do not match.');
    return true;
  }),
], async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('auth/reset-password', { title: 'Reset Password', user: null, token: req.params.token, errors: errors.array(), messages: req.flash() });
  }
  try {
    const user = await User.findOne({
      where: { resetToken: req.params.token, resetExpires: { [require('sequelize').Op.gt]: new Date() } },
    });
    if (!user) {
      req.flash('error', 'Password reset token is invalid or has expired.');
      return res.redirect('/auth/forgot');
    }
    const passwordHash = await bcrypt.hash(req.body.password, 12);
    await user.update({ passwordHash, resetToken: null, resetExpires: null });
    logger.info(`Password reset completed for: ${user.email}`);
    req.flash('success', 'Password updated successfully. Please log in.');
    res.redirect('/auth/login');
  } catch (err) {
    logger.error(`Password reset error: ${err.message}`);
    req.flash('error', 'An error occurred.');
    res.redirect('/auth/forgot');
  }
});

module.exports = router;
