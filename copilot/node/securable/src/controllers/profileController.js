'use strict';

const bcrypt = require('bcrypt');
const { validationResult } = require('express-validator');
const { User } = require('../models');
const { logActivity } = require('../services/auditService');

const BCRYPT_ROUNDS = 12;

function getProfilePage(req, res) {
  res.render('profile/edit', { title: 'Edit Profile', csrfToken: req.csrfToken() });
}

async function putUpdateProfile(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    req.flash('error', errors.array().map((e) => e.msg).join(' '));
    return res.redirect('/profile');
  }
  try {
    const { username, email, currentPassword, newPassword } = req.body;
    const user = await User.scope('withAuth').findByPk(req.user.id);

    const updates = {};
    if (username && username !== user.username) {
      const taken = await User.findOne({ where: { username } });
      if (taken) {
        req.flash('error', 'That username is already taken.');
        return res.redirect('/profile');
      }
      updates.username = username;
    }
    if (email && email.toLowerCase() !== user.email) {
      updates.email = email.toLowerCase();
    }

    if (newPassword) {
      if (!currentPassword) {
        req.flash('error', 'Current password is required to set a new password.');
        return res.redirect('/profile');
      }
      const match = await bcrypt.compare(currentPassword, user.passwordHash);
      if (!match) {
        req.flash('error', 'Current password is incorrect.');
        return res.redirect('/profile');
      }
      updates.passwordHash = await bcrypt.hash(newPassword, BCRYPT_ROUNDS);
    }

    if (Object.keys(updates).length === 0) {
      req.flash('info', 'No changes made.');
      return res.redirect('/profile');
    }

    await user.update(updates);
    await logActivity({ userId: user.id, action: 'user.profile.update', ipAddress: req.ip });
    req.flash('success', 'Profile updated.');
    return res.redirect('/profile');
  } catch (err) {
    return next(err);
  }
}

module.exports = { getProfilePage, putUpdateProfile };
