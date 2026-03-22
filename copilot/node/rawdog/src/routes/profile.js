const express = require('express');
const { asyncHandler } = require('../lib/async-handler');
const { hashPassword, redirectWithSession, verifyPassword } = require('../lib/auth');
const { logActivity } = require('../lib/activity');
const { getDb } = require('../db');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

router.get('/', requireAuth, (req, res) => {
  res.render('profile/edit', {
    pageTitle: 'Profile',
    errors: [],
    formData: {
      username: req.currentUser.username,
      email: req.currentUser.email
    }
  });
});

router.post(
  '/',
  requireAuth,
  asyncHandler(async (req, res) => {
    const db = await getDb();
    const {
      username = '',
      email = '',
      currentPassword = '',
      newPassword = '',
      confirmNewPassword = ''
    } = req.body;
    const errors = [];

    if (!username.trim()) {
      errors.push('Username is required.');
    }

    if (!email.trim()) {
      errors.push('Email address is required.');
    }

    const existingUser = await db.get(
      `
        SELECT id
        FROM users
        WHERE id != ?
          AND (LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?))
      `,
      [req.currentUser.id, username.trim(), email.trim()]
    );

    if (existingUser) {
      errors.push('That username or email address is already in use.');
    }

    let nextPasswordHash = null;

    if (newPassword || confirmNewPassword || currentPassword) {
      if (!newPassword || newPassword.length < 8) {
        errors.push('New password must be at least 8 characters long.');
      }

      if (newPassword !== confirmNewPassword) {
        errors.push('New password confirmation does not match.');
      }

      const currentUserWithPassword = await db.get('SELECT password_hash FROM users WHERE id = ?', [
        req.currentUser.id
      ]);
      const isCurrentPasswordValid = currentUserWithPassword
        ? await verifyPassword(currentPassword, currentUserWithPassword.password_hash)
        : false;

      if (!isCurrentPasswordValid) {
        errors.push('Current password is incorrect.');
      } else {
        nextPasswordHash = await hashPassword(newPassword);
      }
    }

    if (errors.length > 0) {
      res.status(422).render('profile/edit', {
        pageTitle: 'Profile',
        errors,
        formData: {
          username,
          email
        }
      });
      return;
    }

    if (nextPasswordHash) {
      await db.run(
        `
          UPDATE users
          SET username = ?, email = ?, password_hash = ?, updated_at = CURRENT_TIMESTAMP
          WHERE id = ?
        `,
        [username.trim(), email.trim().toLowerCase(), nextPasswordHash, req.currentUser.id]
      );
    } else {
      await db.run(
        `
          UPDATE users
          SET username = ?, email = ?, updated_at = CURRENT_TIMESTAMP
          WHERE id = ?
        `,
        [username.trim(), email.trim().toLowerCase(), req.currentUser.id]
      );
    }

    await logActivity(req.currentUser.id, 'profile_updated', `Profile updated for ${username.trim()}.`);
    req.flash('success', 'Profile updated successfully.');
    await redirectWithSession(req, res, '/profile');
  })
);

module.exports = router;
