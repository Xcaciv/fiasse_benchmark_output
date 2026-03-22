const express = require('express');
const router = express.Router();
const bcrypt = require('bcrypt');
const { body, validationResult } = require('express-validator');
const { User } = require('../models');
const { ensureAuthenticated } = require('../middleware/auth');

// GET /profile
router.get('/', ensureAuthenticated, (req, res) => {
  res.render('profile/edit', { title: 'Edit Profile', user: req.user, errors: [], messages: req.flash() });
});

// POST /profile
router.post('/', ensureAuthenticated, [
  body('username').trim().isLength({ min: 3 }).withMessage('Username must be at least 3 characters.'),
  body('email').isEmail().normalizeEmail().withMessage('Enter a valid email.'),
  body('newPassword').optional({ checkFalsy: true }).isLength({ min: 6 }).withMessage('New password must be at least 6 characters.'),
  body('confirmPassword').custom((value, { req }) => {
    if (req.body.newPassword && value !== req.body.newPassword) throw new Error('Passwords do not match.');
    return true;
  }),
], async (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('profile/edit', { title: 'Edit Profile', user: req.user, errors: errors.array(), messages: req.flash() });
  }
  const { username, email, currentPassword, newPassword } = req.body;
  try {
    // Check if email/username taken by another user
    const emailUser = await User.findOne({ where: { email } });
    if (emailUser && emailUser.id !== req.user.id) {
      return res.render('profile/edit', { title: 'Edit Profile', user: req.user, errors: [{ msg: 'Email already in use.' }], messages: req.flash() });
    }
    const usernameUser = await User.findOne({ where: { username } });
    if (usernameUser && usernameUser.id !== req.user.id) {
      return res.render('profile/edit', { title: 'Edit Profile', user: req.user, errors: [{ msg: 'Username already taken.' }], messages: req.flash() });
    }

    const updates = { username, email };

    if (newPassword) {
      if (!currentPassword) {
        return res.render('profile/edit', { title: 'Edit Profile', user: req.user, errors: [{ msg: 'Current password is required to set a new password.' }], messages: req.flash() });
      }
      const match = await bcrypt.compare(currentPassword, req.user.passwordHash);
      if (!match) {
        return res.render('profile/edit', { title: 'Edit Profile', user: req.user, errors: [{ msg: 'Current password is incorrect.' }], messages: req.flash() });
      }
      updates.passwordHash = await bcrypt.hash(newPassword, 12);
    }

    await req.user.update(updates);
    req.flash('success', 'Profile updated successfully.');
    res.redirect('/profile');
  } catch (err) {
    next(err);
  }
});

module.exports = router;
