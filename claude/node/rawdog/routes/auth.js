const express = require('express');
const router = express.Router();
const passport = require('passport');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');
const { body, validationResult } = require('express-validator');
const db = require('../models');

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
      req.flash('error', info.message || 'Invalid credentials.');
      return res.redirect('/auth/login');
    }
    req.logIn(user, async (err) => {
      if (err) return next(err);
      try {
        await db.AuditLog.create({
          userId: user.id,
          action: 'user_login',
          details: `User ${user.username} logged in`,
          ipAddress: req.ip
        });
      } catch (e) { /* non-fatal */ }
      req.flash('success_msg', `Welcome back, ${user.username}!`);
      res.redirect('/notes');
    });
  })(req, res, next);
});

// GET /auth/register
router.get('/register', (req, res) => {
  if (req.isAuthenticated()) return res.redirect('/notes');
  res.render('auth/register', { title: 'Register', errors: [] });
});

// POST /auth/register
router.post('/register', [
  body('username').trim().isLength({ min: 3, max: 50 }).withMessage('Username must be 3-50 characters.'),
  body('email').trim().isEmail().withMessage('Please provide a valid email address.').normalizeEmail(),
  body('password').isLength({ min: 6 }).withMessage('Password must be at least 6 characters.'),
  body('confirmPassword').custom((value, { req }) => {
    if (value !== req.body.password) throw new Error('Passwords do not match.');
    return true;
  })
], async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.render('auth/register', { title: 'Register', errors: errors.array() });
    }

    const { username, email, password } = req.body;

    const existingUser = await db.User.findOne({ where: { username } });
    if (existingUser) {
      return res.render('auth/register', {
        title: 'Register',
        errors: [{ msg: 'Username already taken.' }]
      });
    }
    const existingEmail = await db.User.findOne({ where: { email } });
    if (existingEmail) {
      return res.render('auth/register', {
        title: 'Register',
        errors: [{ msg: 'Email already registered.' }]
      });
    }

    const hashedPassword = await bcrypt.hash(password, 12);
    const user = await db.User.create({ username, email, password: hashedPassword });

    await db.AuditLog.create({
      userId: user.id,
      action: 'user_register',
      details: `New user registered: ${user.username}`,
      ipAddress: req.ip
    });

    req.flash('success_msg', 'Registration successful! Please log in.');
    res.redirect('/auth/login');
  } catch (err) {
    next(err);
  }
});

// GET /auth/logout
router.get('/logout', (req, res, next) => {
  req.logout((err) => {
    if (err) return next(err);
    req.flash('success_msg', 'You have been logged out.');
    res.redirect('/');
  });
});

// POST /auth/logout
router.post('/logout', (req, res, next) => {
  req.logout((err) => {
    if (err) return next(err);
    req.flash('success_msg', 'You have been logged out.');
    res.redirect('/');
  });
});

// GET /auth/forgot-password
router.get('/forgot-password', (req, res) => {
  res.render('auth/forgot-password', { title: 'Forgot Password' });
});

// POST /auth/forgot-password
router.post('/forgot-password', [
  body('email').trim().isEmail().withMessage('Please provide a valid email address.').normalizeEmail()
], async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      req.flash('error', errors.array()[0].msg);
      return res.redirect('/auth/forgot-password');
    }

    const { email } = req.body;
    const user = await db.User.findOne({ where: { email } });

    if (user) {
      const token = uuidv4();
      const expiry = new Date(Date.now() + 3600000); // 1 hour
      await user.update({ resetToken: token, resetTokenExpiry: expiry });

      const resetUrl = `${process.env.BASE_URL || 'http://localhost:' + (process.env.PORT || 3000)}/auth/reset-password?token=${token}`;
      console.log('\n--- PASSWORD RESET EMAIL ---');
      console.log(`To: ${email}`);
      console.log(`Subject: Password Reset Request`);
      console.log(`Reset link: ${resetUrl}`);
      console.log(`(Link expires in 1 hour)`);
      console.log('----------------------------\n');
    }

    // Always redirect to confirmation to prevent user enumeration
    res.redirect('/auth/forgot-password-confirmation');
  } catch (err) {
    next(err);
  }
});

// GET /auth/forgot-password-confirmation
router.get('/forgot-password-confirmation', (req, res) => {
  res.render('auth/forgot-password-confirmation', { title: 'Check Your Email' });
});

// GET /auth/reset-password
router.get('/reset-password', async (req, res, next) => {
  try {
    const { token } = req.query;
    if (!token) {
      req.flash('error', 'Invalid reset link.');
      return res.redirect('/auth/forgot-password');
    }

    const user = await db.User.findOne({ where: { resetToken: token } });
    if (!user || !user.resetTokenExpiry || user.resetTokenExpiry < new Date()) {
      req.flash('error', 'Invalid or expired reset link. Please request a new one.');
      return res.redirect('/auth/forgot-password');
    }

    res.render('auth/reset-password', { title: 'Reset Password', token, errors: [] });
  } catch (err) {
    next(err);
  }
});

// POST /auth/reset-password
router.post('/reset-password', [
  body('password').isLength({ min: 6 }).withMessage('Password must be at least 6 characters.'),
  body('confirmPassword').custom((value, { req }) => {
    if (value !== req.body.password) throw new Error('Passwords do not match.');
    return true;
  })
], async (req, res, next) => {
  try {
    const errors = validationResult(req);
    const { token, password } = req.body;

    if (!errors.isEmpty()) {
      return res.render('auth/reset-password', { title: 'Reset Password', token, errors: errors.array() });
    }

    const user = await db.User.findOne({ where: { resetToken: token } });
    if (!user || !user.resetTokenExpiry || user.resetTokenExpiry < new Date()) {
      req.flash('error', 'Invalid or expired reset link. Please request a new one.');
      return res.redirect('/auth/forgot-password');
    }

    const hashedPassword = await bcrypt.hash(password, 12);
    await user.update({ password: hashedPassword, resetToken: null, resetTokenExpiry: null });

    await db.AuditLog.create({
      userId: user.id,
      action: 'password_reset',
      details: `Password reset for user: ${user.username}`,
      ipAddress: req.ip
    });

    res.redirect('/auth/reset-password-confirmation');
  } catch (err) {
    next(err);
  }
});

// GET /auth/reset-password-confirmation
router.get('/reset-password-confirmation', (req, res) => {
  res.render('auth/reset-password-confirmation', { title: 'Password Reset Successful' });
});

module.exports = router;
