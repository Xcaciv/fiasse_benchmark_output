const crypto = require('crypto');
const express = require('express');
const { asyncHandler } = require('../lib/async-handler');
const {
  clearAuthCookie,
  destroySession,
  hashPassword,
  redirectWithSession,
  setAuthCookie,
  verifyPassword
} = require('../lib/auth');
const { logActivity } = require('../lib/activity');
const { sendPasswordResetEmail } = require('../lib/mailer');
const { getDb, withTransaction } = require('../db');
const { config } = require('../config');
const { redirectIfAuthenticated, requireAuth } = require('../middleware/auth');

const router = express.Router();

function validatePassword(password, confirmPassword) {
  const errors = [];

  if (!password || password.length < 8) {
    errors.push('Password must be at least 8 characters long.');
  }

  if (password !== confirmPassword) {
    errors.push('Password confirmation does not match.');
  }

  return errors;
}

router.get('/register', redirectIfAuthenticated, (req, res) => {
  res.render('auth/register', {
    pageTitle: 'Register',
    errors: [],
    formData: {}
  });
});

router.post(
  '/register',
  redirectIfAuthenticated,
  asyncHandler(async (req, res) => {
    const db = await getDb();
    const { username = '', email = '', password = '', confirmPassword = '' } = req.body;
    const errors = [];

    if (!username.trim()) {
      errors.push('Username is required.');
    }

    if (!email.trim()) {
      errors.push('Email address is required.');
    }

    errors.push(...validatePassword(password, confirmPassword));

    const existingUser =
      username.trim() || email.trim()
        ? await db.get(
            'SELECT id FROM users WHERE LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?)',
            [username.trim(), email.trim()]
          )
        : null;

    if (existingUser) {
      errors.push('A user with that username or email already exists.');
    }

    if (errors.length > 0) {
      res.status(422).render('auth/register', {
        pageTitle: 'Register',
        errors,
        formData: { username, email }
      });
      return;
    }

    const passwordHash = await hashPassword(password);
    const result = await db.run(
      `
        INSERT INTO users (username, email, password_hash)
        VALUES (?, ?, ?)
      `,
      [username.trim(), email.trim().toLowerCase(), passwordHash]
    );

    const user = await db.get(
      'SELECT id, username, email, role, created_at, updated_at FROM users WHERE id = ?',
      [result.lastID]
    );

    setAuthCookie(res, user.id);
    await logActivity(user.id, 'register', `User ${user.username} registered.`);
    req.flash('success', 'Account created successfully.');
    await redirectWithSession(req, res, '/');
  })
);

router.get('/login', redirectIfAuthenticated, (req, res) => {
  res.render('auth/login', {
    pageTitle: 'Login',
    errors: [],
    formData: {}
  });
});

router.post(
  '/login',
  redirectIfAuthenticated,
  asyncHandler(async (req, res) => {
    const db = await getDb();
    const { identifier = '', password = '' } = req.body;
    const errors = [];

    if (!identifier.trim() || !password) {
      errors.push('Username or email and password are required.');
    }

    const user = identifier.trim()
      ? await db.get(
          `
            SELECT *
            FROM users
            WHERE LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?)
          `,
          [identifier.trim(), identifier.trim()]
        )
      : null;

    const isPasswordValid =
      user && password ? await verifyPassword(password, user.password_hash) : false;

    if (!user || !isPasswordValid) {
      await logActivity(null, 'login_failed', `Failed login attempt for ${identifier.trim() || 'unknown'}.`);
      errors.push('Invalid username/email or password.');
    }

    if (errors.length > 0) {
      res.status(422).render('auth/login', {
        pageTitle: 'Login',
        errors,
        formData: { identifier }
      });
      return;
    }

    setAuthCookie(res, user.id);
    await logActivity(user.id, 'login_success', `User ${user.username} logged in.`);
    req.flash('success', `Welcome back, ${user.username}.`);
    await redirectWithSession(req, res, '/');
  })
);

router.post(
  '/logout',
  requireAuth,
  asyncHandler(async (req, res) => {
    const username = req.currentUser.username;
    const userId = req.currentUser.id;
    await destroySession(req);
    clearAuthCookie(res);
    res.clearCookie('loose-notes.sid');
    await logActivity(userId, 'logout', `User ${username} logged out.`);
    res.redirect('/auth/login');
  })
);

router.get('/forgot-password', redirectIfAuthenticated, (req, res) => {
  res.render('auth/forgot-password', {
    pageTitle: 'Forgot password',
    errors: []
  });
});

router.post(
  '/forgot-password',
  redirectIfAuthenticated,
  asyncHandler(async (req, res) => {
    const db = await getDb();
    const email = (req.body.email || '').trim().toLowerCase();
    const errors = [];

    if (!email) {
      errors.push('Email address is required.');
    }

    if (errors.length > 0) {
      res.status(422).render('auth/forgot-password', {
        pageTitle: 'Forgot password',
        errors
      });
      return;
    }

    const user = await db.get('SELECT id, username, email FROM users WHERE LOWER(email) = LOWER(?)', [email]);

    if (user) {
      const rawToken = crypto.randomBytes(32).toString('hex');
      const tokenHash = crypto.createHash('sha256').update(rawToken).digest('hex');
      const expiresAt = new Date(Date.now() + 60 * 60 * 1000).toISOString();

      await withTransaction(async (database) => {
        await database.run(
          `
            UPDATE password_reset_tokens
            SET used_at = CURRENT_TIMESTAMP
            WHERE user_id = ? AND used_at IS NULL
          `,
          [user.id]
        );

        await database.run(
          `
            INSERT INTO password_reset_tokens (user_id, token_hash, expires_at)
            VALUES (?, ?, ?)
          `,
          [user.id, tokenHash, expiresAt]
        );
      });

      const resetUrl = `${config.baseUrl}/auth/reset-password/${rawToken}`;
      const outboxPath = await sendPasswordResetEmail(user, resetUrl);
      await logActivity(user.id, 'password_reset_requested', `Password reset requested. Email stored at ${outboxPath}.`);
    }

    req.flash(
      'success',
      'If an account exists for that email address, a password reset message has been queued in the local outbox.'
    );
    await redirectWithSession(req, res, '/auth/login');
  })
);

router.get(
  '/reset-password/:token',
  redirectIfAuthenticated,
  asyncHandler(async (req, res) => {
    const db = await getDb();
    const tokenHash = crypto.createHash('sha256').update(req.params.token).digest('hex');
    const resetRecord = await db.get(
      `
        SELECT prt.id
        FROM password_reset_tokens prt
        WHERE prt.token_hash = ?
          AND prt.used_at IS NULL
          AND datetime(prt.expires_at) > datetime('now')
      `,
      [tokenHash]
    );

    if (!resetRecord) {
      res.status(400).render('auth/reset-password', {
        pageTitle: 'Reset password',
        errors: ['This password reset link is invalid or has expired.']
      });
      return;
    }

    res.render('auth/reset-password', {
      pageTitle: 'Reset password',
      errors: []
    });
  })
);

router.post(
  '/reset-password/:token',
  redirectIfAuthenticated,
  asyncHandler(async (req, res) => {
    const db = await getDb();
    const { password = '', confirmPassword = '' } = req.body;
    const tokenHash = crypto.createHash('sha256').update(req.params.token).digest('hex');
    const errors = validatePassword(password, confirmPassword);

    const resetRecord = await db.get(
      `
        SELECT prt.id, prt.user_id, u.username
        FROM password_reset_tokens prt
        JOIN users u ON u.id = prt.user_id
        WHERE prt.token_hash = ?
          AND prt.used_at IS NULL
          AND datetime(prt.expires_at) > datetime('now')
      `,
      [tokenHash]
    );

    if (!resetRecord) {
      errors.push('This password reset link is invalid or has expired.');
    }

    if (errors.length > 0) {
      res.status(422).render('auth/reset-password', {
        pageTitle: 'Reset password',
        errors
      });
      return;
    }

    const passwordHash = await hashPassword(password);

    await withTransaction(async (database) => {
      await database.run('UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?', [
        passwordHash,
        resetRecord.user_id
      ]);
      await database.run('UPDATE password_reset_tokens SET used_at = CURRENT_TIMESTAMP WHERE id = ?', [
        resetRecord.id
      ]);
    });

    await logActivity(resetRecord.user_id, 'password_reset_completed', `Password reset completed for ${resetRecord.username}.`);
    req.flash('success', 'Your password has been reset. You can now log in.');
    await redirectWithSession(req, res, '/auth/login');
  })
);

module.exports = router;
