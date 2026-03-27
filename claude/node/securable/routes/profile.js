'use strict';

const express = require('express');
const bcrypt = require('bcrypt');
const { body } = require('express-validator');
const { User } = require('../models');
const { requireAuth } = require('../middleware/auth');
const { handleValidation } = require('../middleware/validate');
const audit = require('../services/auditService');
const security = require('../config/security');

const router = express.Router();

router.get('/edit', requireAuth, (req, res) => {
  res.render('profile/edit', {
    title: 'Edit Profile',
    user: req.user,
    csrfToken: req.csrfToken(),
  });
});

const profileRules = [
  body('username')
    .trim()
    .isLength({ min: security.USERNAME_MIN_LENGTH, max: security.USERNAME_MAX_LENGTH })
    .withMessage(`Username must be ${security.USERNAME_MIN_LENGTH}–${security.USERNAME_MAX_LENGTH} characters`)
    .matches(/^[a-zA-Z0-9_-]+$/)
    .withMessage('Username may only contain letters, numbers, underscores, and hyphens'),
  body('email')
    .trim()
    .isEmail()
    .withMessage('Valid email required')
    .normalizeEmail(),
  body('newPassword')
    .optional({ checkFalsy: true })
    .isLength({ min: security.PASSWORD_MIN_LENGTH, max: security.PASSWORD_MAX_LENGTH })
    .withMessage(`Password must be ${security.PASSWORD_MIN_LENGTH}–${security.PASSWORD_MAX_LENGTH} characters`),
  body('confirmPassword')
    .custom((val, { req }) => {
      if (req.body.newPassword && val !== req.body.newPassword) {
        throw new Error('Passwords do not match');
      }
      return true;
    }),
];

router.post('/edit', requireAuth, profileRules, handleValidation, async (req, res, next) => {
  try {
    const { username, email, newPassword } = req.body;
    const normalizedUsername = username.trim().toLowerCase();
    const normalizedEmail = email.trim().toLowerCase();

    // Check username/email uniqueness against other users
    const conflictUser = await User.findOne({
      where: { username: normalizedUsername },
    });
    if (conflictUser && conflictUser.id !== req.user.id) {
      req.flash('error', 'Username already taken.');
      return res.redirect('/profile/edit');
    }

    const conflictEmail = await User.findOne({ where: { email: normalizedEmail } });
    if (conflictEmail && conflictEmail.id !== req.user.id) {
      req.flash('error', 'Email already in use.');
      return res.redirect('/profile/edit');
    }

    const updates = { username: normalizedUsername, email: normalizedEmail };
    if (newPassword) {
      updates.passwordHash = await bcrypt.hash(newPassword, security.BCRYPT_ROUNDS);
    }

    await User.update(updates, { where: { id: req.user.id } });
    await audit.record('profile.update', req.user.id, {}, req.ip);
    req.flash('success', 'Profile updated.');
    return res.redirect('/profile/edit');
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
