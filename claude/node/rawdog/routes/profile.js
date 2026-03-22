const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const { getDb } = require('../database/db');
const { ensureAuthenticated } = require('../middleware/auth');

// GET /profile/edit
router.get('/edit', ensureAuthenticated, (req, res) => {
  res.render('profile/edit', { title: 'Edit Profile' });
});

// POST /profile/edit
router.post('/edit', ensureAuthenticated, (req, res) => {
  const db = getDb();
  const { username, email, currentPassword, newPassword, confirmNewPassword } = req.body;

  if (!username || !email) {
    req.flash('error', 'Username and email are required.');
    return res.redirect('/profile/edit');
  }

  // Check uniqueness (excluding current user)
  const existing = db.prepare(
    'SELECT id FROM users WHERE (username = ? OR email = ?) AND id != ?'
  ).get(username, email, req.user.id);

  if (existing) {
    req.flash('error', 'Username or email is already in use by another account.');
    return res.redirect('/profile/edit');
  }

  // If changing password
  if (newPassword) {
    if (!currentPassword) {
      req.flash('error', 'Current password is required to set a new password.');
      return res.redirect('/profile/edit');
    }

    if (!bcrypt.compareSync(currentPassword, req.user.password_hash)) {
      req.flash('error', 'Current password is incorrect.');
      return res.redirect('/profile/edit');
    }

    if (newPassword !== confirmNewPassword) {
      req.flash('error', 'New passwords do not match.');
      return res.redirect('/profile/edit');
    }

    if (newPassword.length < 6) {
      req.flash('error', 'New password must be at least 6 characters.');
      return res.redirect('/profile/edit');
    }

    const hash = bcrypt.hashSync(newPassword, 10);
    db.prepare(
      'UPDATE users SET username = ?, email = ?, password_hash = ? WHERE id = ?'
    ).run(username, email, hash, req.user.id);
  } else {
    db.prepare(
      'UPDATE users SET username = ?, email = ? WHERE id = ?'
    ).run(username, email, req.user.id);
  }

  req.flash('success', 'Profile updated successfully.');
  res.redirect('/profile/edit');
});

module.exports = router;
