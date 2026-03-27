'use strict';
const userModel = require('../models/userModel');
const passwordService = require('../services/passwordService');
const auditService = require('../services/auditService');
const { validateEmail, validateUsername } = require('../utils/validation');

function getProfile(req, res, db) {
  const user = userModel.findById(db, req.session.userId);
  if (!user) return res.status(404).render('errors/404', {});
  res.render('profile/edit', { user, error: null, success: null });
}

async function postProfile(req, res, db) {
  const user = userModel.findById(db, req.session.userId);
  if (!user) return res.status(404).render('errors/404', {});

  const { username, email, currentPassword, newPassword, confirmNewPassword } = req.body;

  // Require current password for any changes
  const passwordValid = await passwordService.verifyPassword(currentPassword, user.password_hash);
  if (!passwordValid) {
    return res.render('profile/edit', { user, error: 'Current password is incorrect.', success: null });
  }

  const updates = {};

  if (username && username !== user.username) {
    const check = validateUsername(username);
    if (!check.valid) return res.render('profile/edit', { user, error: check.reason, success: null });
    updates.username = username;
  }
  if (email && email !== user.email) {
    const check = validateEmail(email);
    if (!check.valid) return res.render('profile/edit', { user, error: check.reason, success: null });
    updates.email = email;
  }

  if (newPassword) {
    if (newPassword !== confirmNewPassword) {
      return res.render('profile/edit', { user, error: 'New passwords do not match.', success: null });
    }
    const passCheck = passwordService.validatePasswordPolicy(newPassword);
    if (!passCheck.valid) {
      return res.render('profile/edit', { user, error: passCheck.reason, success: null });
    }
    const newHash = await passwordService.hashPassword(newPassword);
    userModel.updatePassword(db, user.id, newHash);
    auditService.log({ eventType: 'PASSWORD_CHANGED', userId: user.id, ipAddress: req.ip });
  }

  if (Object.keys(updates).length > 0) {
    userModel.updateUser(db, user.id, updates);
    if (updates.username) req.session.username = updates.username;
    auditService.log({ eventType: 'PROFILE_UPDATED', userId: user.id, ipAddress: req.ip });
  }

  const updatedUser = userModel.findById(db, user.id);
  res.render('profile/edit', { user: updatedUser, error: null, success: 'Profile updated successfully.' });
}

module.exports = { getProfile, postProfile };
