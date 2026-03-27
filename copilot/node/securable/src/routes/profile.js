'use strict';

const express = require('express');
const { requireAuth } = require('../middleware/authenticate');
const { profileValidation } = require('../middleware/validate');
const { User } = require('../models/index');

const router = express.Router();

// GET /profile
router.get('/', requireAuth, async (req, res, next) => {
  try {
    const profileUser = await User.findByPk(req.session.userId, {
      attributes: ['id', 'username', 'email', 'role', 'createdAt'],
    });

    if (!profileUser) return res.redirect('/auth/login');

    res.render('profile/edit', {
      title: 'My Profile',
      profileUser,
      user: res.locals.user,
    });
  } catch (err) {
    next(err);
  }
});

// PUT /profile
router.put('/', requireAuth, profileValidation, async (req, res, next) => {
  try {
    const { username, email, password } = req.body;

    const user = await User.findByPk(req.session.userId);
    if (!user) return res.redirect('/auth/login');

    const updates = { username, email };

    // Only rehash if a new password is provided
    if (password) {
      updates.passwordHash = await User.hashPassword(password);
    }

    await user.update(updates);

    req.session.flash = { type: 'success', message: 'Profile updated.' };
    res.redirect('/profile');
  } catch (err) {
    if (err.name === 'SequelizeUniqueConstraintError') {
      req.session.flash = { type: 'error', message: 'Username or email is already taken.' };
      return res.redirect('/profile');
    }
    next(err);
  }
});

module.exports = router;
