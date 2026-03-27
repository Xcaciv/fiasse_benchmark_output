'use strict';

const express = require('express');
const { body, validationResult } = require('express-validator');
const { v4: uuidv4 } = require('uuid');
const { User } = require('../models');
const authService = require('../services/authService');
const auditService = require('../services/auditService');
const emailService = require('../services/emailService');
const { registrationLimiter, loginLimiter, passwordResetLimiter } = require('../middleware/rateLimiter');
const { logger } = require('../config/logger');
const constants = require('../config/constants');

const router = express.Router();

// ─── Registration ────────────────────────────────────────────────────────────

router.get('/register', (req, res) => {
  if (req.session.userId) return res.redirect('/notes');
  res.render('auth/register', {
    title: 'Register',
    csrfToken: req.csrfToken(),
    errors: [],
    currentUser: null
  });
});

router.post('/register', registrationLimiter, [
  body('username').trim().isLength({ min: 3, max: 50 }).matches(/^[a-zA-Z0-9_-]+$/),
  body('email').isEmail().normalizeEmail(),
  body('password').isLength({ min: constants.AUTH.MIN_PASSWORD_LENGTH })
], async (req, res, next) => {
  const errors = validationResult(req);

  if (!errors.isEmpty()) {
    return res.status(422).render('auth/register', {
      title: 'Register',
      csrfToken: req.csrfToken(),
      errors: errors.array(),
      currentUser: null
    });
  }

  try {
    const { username, email, password } = req.body;

    const { valid, message } = authService.validatePasswordStrength(password);
    if (!valid) {
      return res.status(422).render('auth/register', {
        title: 'Register',
        csrfToken: req.csrfToken(),
        errors: [{ msg: message }],
        currentUser: null
      });
    }

    const existingUser = await User.scope('withAuth').findOne({
      where: { email }
    });

    if (existingUser) {
      // Availability: neutral message to prevent user enumeration
      await auditService.log('auth.register_duplicate', {
        targetId: null,
        outcome: 'failure',
        ip: req.ip,
        correlationId: req.correlationId
      });
      req.flash('info', 'If that email is available, your account has been created.');
      return res.redirect('/auth/login');
    }

    const passwordHash = await authService.hashPassword(password);

    const user = await User.create({
      username,
      email,
      passwordHash,
      role: constants.ROLES.USER,
      isActive: true,
      emailVerified: false
    });

    await auditService.log('auth.register', {
      actorId: user.id,
      targetId: user.id,
      targetType: 'user',
      outcome: 'success',
      ip: req.ip,
      correlationId: req.correlationId
    });

    req.flash('success', 'Account created. Please log in.');
    res.redirect('/auth/login');
  } catch (err) {
    next(err);
  }
});

// ─── Login ───────────────────────────────────────────────────────────────────

router.get('/login', (req, res) => {
  if (req.session.userId) return res.redirect('/notes');
  res.render('auth/login', {
    title: 'Login',
    csrfToken: req.csrfToken(),
    errors: [],
    currentUser: null
  });
});

router.post('/login', loginLimiter, [
  body('email').isEmail().normalizeEmail(),
  body('password').notEmpty()
], async (req, res, next) => {
  const errors = validationResult(req);

  if (!errors.isEmpty()) {
    return res.status(422).render('auth/login', {
      title: 'Login',
      csrfToken: req.csrfToken(),
      errors: errors.array(),
      currentUser: null
    });
  }

  try {
    const { email, password } = req.body;

    const user = await User.scope('withAuth').findOne({ where: { email } });

    if (!user || !user.isActive) {
      // Authenticity: same message for missing and inactive users (no enumeration)
      return renderLoginFailure(req, res, 'Invalid credentials.');
    }

    const { locked, until } = authService.checkAccountLockout(user);
    if (locked) {
      await auditService.log('auth.login_locked', {
        actorId: user.id,
        outcome: 'denied',
        ip: req.ip,
        correlationId: req.correlationId
      });
      return renderLoginFailure(req, res, `Account locked until ${until.toISOString()}`);
    }

    const valid = await authService.verifyPassword(password, user.passwordHash);
    if (!valid) {
      await authService.recordFailedLogin(user);
      await auditService.log('auth.login_failed', {
        actorId: user.id,
        outcome: 'failure',
        ip: req.ip,
        correlationId: req.correlationId
      });
      return renderLoginFailure(req, res, 'Invalid credentials.');
    }

    await authService.resetLoginAttempts(user);

    // Authenticity: regenerate session ID on login to prevent session fixation
    req.session.regenerate(async (err) => {
      if (err) return next(err);

      req.session.userId = user.id;
      req.session.loginAt = Date.now();

      await auditService.log('auth.login', {
        actorId: user.id,
        targetId: user.id,
        targetType: 'user',
        outcome: 'success',
        ip: req.ip,
        correlationId: req.correlationId
      });

      res.redirect('/notes');
    });
  } catch (err) {
    next(err);
  }
});

function renderLoginFailure(req, res, message) {
  return res.status(401).render('auth/login', {
    title: 'Login',
    csrfToken: req.csrfToken(),
    errors: [{ msg: message }],
    currentUser: null
  });
}

// ─── Logout ──────────────────────────────────────────────────────────────────

router.post('/logout', async (req, res, next) => {
  const userId = req.session.userId;
  const correlationId = req.correlationId;
  const ip = req.ip;

  req.session.destroy(async (err) => {
    if (err) {
      logger.error('Session destroy error', { event: 'auth.logout_error', error: err.message });
    }

    if (userId) {
      await auditService.log('auth.logout', {
        actorId: userId,
        outcome: 'success',
        ip,
        correlationId
      });
    }

    res.clearCookie('connect.sid');
    res.redirect('/auth/login');
  });
});

// ─── Forgot Password ─────────────────────────────────────────────────────────

router.get('/forgot-password', (req, res) => {
  res.render('auth/forgot-password', {
    title: 'Forgot Password',
    csrfToken: req.csrfToken(),
    errors: [],
    currentUser: null
  });
});

router.post('/forgot-password', passwordResetLimiter, [
  body('email').isEmail().normalizeEmail()
], async (req, res, next) => {
  // Availability: always return neutral response to prevent email enumeration
  const neutralResponse = () => {
    req.flash('info', 'If an account with that email exists, a reset link has been sent.');
    res.redirect('/auth/forgot-password');
  };

  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return neutralResponse();

    const { email } = req.body;
    const user = await User.scope('withAuth').findOne({ where: { email, isActive: true } });

    if (!user) {
      emailService.sendPasswordResetRequestNotification(email);
      return neutralResponse();
    }

    const { token, hash, expiresAt } = authService.generatePasswordResetToken();
    await user.update({ passwordResetHash: hash, passwordResetExpiry: expiresAt });

    const resetUrl = `${req.protocol}://${req.get('host')}/auth/reset-password/${token}`;
    emailService.sendPasswordResetEmail(email, resetUrl);

    await auditService.log('auth.password_reset_requested', {
      actorId: user.id,
      outcome: 'success',
      ip: req.ip,
      correlationId: req.correlationId
    });

    neutralResponse();
  } catch (err) {
    next(err);
  }
});

// ─── Reset Password ──────────────────────────────────────────────────────────

router.get('/reset-password/:token', async (req, res, next) => {
  try {
    const user = await authService.findUserByResetToken(req.params.token);
    if (!user) {
      req.flash('error', 'Password reset link is invalid or has expired.');
      return res.redirect('/auth/forgot-password');
    }

    res.render('auth/reset-password', {
      title: 'Reset Password',
      csrfToken: req.csrfToken(),
      token: req.params.token,
      errors: [],
      currentUser: null
    });
  } catch (err) {
    next(err);
  }
});

router.post('/reset-password/:token', [
  body('password').isLength({ min: constants.AUTH.MIN_PASSWORD_LENGTH }),
  body('confirmPassword').notEmpty()
], async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(422).render('auth/reset-password', {
        title: 'Reset Password',
        csrfToken: req.csrfToken(),
        token: req.params.token,
        errors: errors.array(),
        currentUser: null
      });
    }

    const { password, confirmPassword } = req.body;

    if (password !== confirmPassword) {
      return res.status(422).render('auth/reset-password', {
        title: 'Reset Password',
        csrfToken: req.csrfToken(),
        token: req.params.token,
        errors: [{ msg: 'Passwords do not match.' }],
        currentUser: null
      });
    }

    const user = await authService.findUserByResetToken(req.params.token);
    if (!user) {
      req.flash('error', 'Password reset link is invalid or has expired.');
      return res.redirect('/auth/forgot-password');
    }

    const { valid, message } = authService.validatePasswordStrength(password);
    if (!valid) {
      return res.status(422).render('auth/reset-password', {
        title: 'Reset Password',
        csrfToken: req.csrfToken(),
        token: req.params.token,
        errors: [{ msg: message }],
        currentUser: null
      });
    }

    const passwordHash = await authService.hashPassword(password);
    await user.update({
      passwordHash,
      passwordResetHash: null,
      passwordResetExpiry: null,
      failedLoginAttempts: 0,
      lockoutUntil: null
    });

    emailService.sendPasswordChangeNotification(user.email);

    await auditService.log('auth.password_reset', {
      actorId: user.id,
      outcome: 'success',
      ip: req.ip,
      correlationId: req.correlationId
    });

    req.flash('success', 'Password updated. Please log in.');
    res.redirect('/auth/login');
  } catch (err) {
    next(err);
  }
});

module.exports = router;
