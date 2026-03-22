'use strict';
const bcrypt = require('bcryptjs');
const { validationResult } = require('express-validator');
const { User } = require('../models');
const { BCRYPT_SALT_ROUNDS } = require('../config/constants');
const { createAuditService } = require('../services/auditService');
const logger = require('../utils/logger');
const { canonicalize } = require('../utils/inputHandler');

const audit = createAuditService(logger);

async function showProfile(req, res, next) {
  return res.render('profile/edit', { user: req.user, errors: [], csrfToken: req.csrfToken() });
}

async function updateProfile(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('profile/edit', { user: req.user, errors: errors.array(), csrfToken: req.csrfToken() });
  }
  const { username, email, newPassword } = req.body;
  const updates = {
    username: canonicalize(username).toLowerCase(),
    email: canonicalize(email).toLowerCase()
  };
  if (newPassword && newPassword.length >= 8) {
    updates.passwordHash = await bcrypt.hash(newPassword, BCRYPT_SALT_ROUNDS);
  }
  await User.update(updates, { where: { id: req.user.id } });
  audit.logAuthEvent('PROFILE_UPDATE', req.user.id, req.user.username, req.ip, true, {});
  req.flash('success', 'Profile updated.');
  return res.redirect('/profile');
}

module.exports = { showProfile, updateProfile };
