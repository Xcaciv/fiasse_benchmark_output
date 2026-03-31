'use strict';

const express = require('express');
const { body } = require('express-validator');
const passport = require('../config/passport');
const authService = require('../services/authService');
const auditService = require('../services/auditService');
const { handleValidationErrors } = require('../middleware/validate');
const { redirectIfAuthenticated } = require('../middleware/auth');
const { loginLimiter } = require('../middleware/rateLimiter');
const logger = require('../config/logger');

const router = express.Router();

// GET /auth/login
router.get('/login', redirectIfAuthenticated, (req, res) => {
  res.render('auth/login', { title: 'Login' });
});

// POST /auth/login
router.post(
  '/login',
  redirectIfAuthenticated,
  loginLimiter,
  [
    body('username').trim().notEmpty().withMessage('Username is required.'),
    body('password').notEmpty().withMessage('Password is required.'),
  ],
  handleValidationErrors,
  passport.authenticate('local', {
    failureRedirect: '/auth/login',
    failureFlash: true,
  }),
  (req, res) => {
    logger.audit('LOGIN_SUCCESS', { userId: req.user.id, ip: req.ip });
    auditService.record({ userId: req.user.id, action: 'LOGIN', ipAddress: req.ip });
    res.redirect('/notes');
  }
);

// POST /auth/logout
router.post('/logout', (req, res, next) => {
  const userId = req.user?.id;
  req.logout((err) => {
    if (err) return next(err);
    req.session.destroy(() => {
      logger.audit('LOGOUT', { userId });
      auditService.record({ userId, action: 'LOGOUT', ipAddress: req.ip });
      res.redirect('/auth/login');
    });
  });
});

// GET /auth/register
router.get('/register', redirectIfAuthenticated, (req, res) => {
  res.render('auth/register', { title: 'Register' });
});

// POST /auth/register
router.post(
  '/register',
  redirectIfAuthenticated,
  [
    body('username')
      .trim()
      .isLength({ min: 3, max: 64 })
      .withMessage('Username must be 3–64 characters.')
      .matches(/^[a-z0-9_.-]+$/i)
      .withMessage('Username may only contain letters, numbers, underscores, hyphens, and dots.'),
    body('email').trim().isEmail().normalizeEmail().withMessage('Valid email is required.'),
    body('password')
      .isLength({ min: 8 })
      .withMessage('Password must be at least 8 characters.')
      .matches(/[A-Z]/)
      .withMessage('Password must contain at least one uppercase letter.')
      .matches(/[0-9]/)
      .withMessage('Password must contain at least one number.'),
    body('confirmPassword').custom((value, { req }) => {
      if (value !== req.body.password) throw new Error('Passwords do not match.');
      return true;
    }),
  ],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      // Request Surface Minimization: extract only expected fields
      const { username, email, password } = req.body;
      await authService.registerUser({ username, email, password });
      req.flash('success', 'Account created. Please log in.');
      res.redirect('/auth/login');
    } catch (err) {
      if (err.status === 409) {
        req.flash('error', err.message);
        return res.redirect('/auth/register');
      }
      next(err);
    }
  }
);

// GET /auth/forgot-password
router.get('/forgot-password', redirectIfAuthenticated, (req, res) => {
  res.render('auth/forgot-password', { title: 'Forgot Password' });
});

// POST /auth/forgot-password
router.post(
  '/forgot-password',
  loginLimiter,
  [body('email').trim().isEmail().normalizeEmail().withMessage('Valid email is required.')],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      const { email } = req.body;
      const baseUrl = `${req.protocol}://${req.get('host')}`;
      await authService.initiatePasswordReset(email, baseUrl);
      // Always show same message to prevent email enumeration
      req.flash('success', 'If that address is registered, a reset link has been sent.');
      res.redirect('/auth/forgot-password');
    } catch (err) {
      next(err);
    }
  }
);

// GET /auth/reset-password
router.get('/reset-password', redirectIfAuthenticated, (req, res) => {
  const { token, uid } = req.query;
  if (!token || !uid) {
    req.flash('error', 'Invalid reset link.');
    return res.redirect('/auth/forgot-password');
  }
  res.render('auth/reset-password', { title: 'Reset Password', token, uid });
});

// POST /auth/reset-password
router.post(
  '/reset-password',
  loginLimiter,
  [
    body('uid').trim().notEmpty().withMessage('Invalid reset link.'),
    body('token').trim().notEmpty().withMessage('Invalid reset link.'),
    body('password')
      .isLength({ min: 8 })
      .withMessage('Password must be at least 8 characters.')
      .matches(/[A-Z]/)
      .withMessage('Password must contain at least one uppercase letter.')
      .matches(/[0-9]/)
      .withMessage('Password must contain at least one number.'),
    body('confirmPassword').custom((value, { req }) => {
      if (value !== req.body.password) throw new Error('Passwords do not match.');
      return true;
    }),
  ],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      const { uid, token, password } = req.body;
      await authService.resetPassword(uid, token, password);
      req.flash('success', 'Password reset successfully. Please log in.');
      res.redirect('/auth/login');
    } catch (err) {
      if (err.status === 400) {
        req.flash('error', err.message);
        return res.redirect(`/auth/reset-password?token=${encodeURIComponent(req.body.token)}&uid=${encodeURIComponent(req.body.uid)}`);
      }
      next(err);
    }
  }
);

module.exports = router;
