const express = require('express');
const bcrypt = require('bcryptjs');
const { User } = require('../models');
const { isAuthenticated } = require('../middleware/auth');

const router = express.Router();

// GET /profile
router.get('/', isAuthenticated, async (req, res) => {
  try {
    const user = await User.findByPk(req.session.userId);
    if (!user) return res.redirect('/auth/login');
    res.render('profile/index', { title: 'My Profile', profileUser: user });
  } catch (err) {
    console.error(err);
    req.flash('error', 'Failed to load profile.');
    res.redirect('/notes');
  }
});

// POST /profile
router.post('/', isAuthenticated, async (req, res) => {
  try {
    const user = await User.findByPk(req.session.userId);
    if (!user) return res.redirect('/auth/login');

    const { username, email, currentPassword, newPassword, confirmNewPassword } = req.body;

    if (!username || !email) {
      req.flash('error', 'Username and email are required.');
      return res.redirect('/profile');
    }

    // Check for conflicts
    const existingUser = await User.findOne({ where: { username } });
    if (existingUser && existingUser.id !== user.id) {
      req.flash('error', 'Username already taken.');
      return res.redirect('/profile');
    }
    const existingEmail = await User.findOne({ where: { email } });
    if (existingEmail && existingEmail.id !== user.id) {
      req.flash('error', 'Email already registered.');
      return res.redirect('/profile');
    }

    const updates = { username, email };

    if (newPassword) {
      if (!currentPassword) {
        req.flash('error', 'Current password is required to change password.');
        return res.redirect('/profile');
      }
      const valid = await bcrypt.compare(currentPassword, user.password);
      if (!valid) {
        req.flash('error', 'Current password is incorrect.');
        return res.redirect('/profile');
      }
      if (newPassword !== confirmNewPassword) {
        req.flash('error', 'New passwords do not match.');
        return res.redirect('/profile');
      }
      if (newPassword.length < 6) {
        req.flash('error', 'New password must be at least 6 characters.');
        return res.redirect('/profile');
      }
      updates.password = await bcrypt.hash(newPassword, 12);
    }

    await user.update(updates);
    req.session.username = username;
    req.flash('success', 'Profile updated.');
    res.redirect('/profile');
  } catch (err) {
    console.error(err);
    req.flash('error', 'Failed to update profile.');
    res.redirect('/profile');
  }
});

module.exports = router;
