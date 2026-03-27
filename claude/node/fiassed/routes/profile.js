'use strict';

const express = require('express');
const { body, validationResult } = require('express-validator');
const { authenticate } = require('../middleware/authenticate');
const { noCacheForPrivate } = require('../middleware/security');
const { User } = require('../models');
const authService = require('../services/authService');
const auditService = require('../services/auditService');
const emailService = require('../services/emailService');
const constants = require('../config/constants');

const router = express.Router();

// ─── View/Edit Profile ────────────────────────────────────────────────────────

router.get('/edit', authenticate, noCacheForPrivate, async (req, res) => {
  res.render('profile/edit', {
    title: 'Edit Profile',
    csrfToken: req.csrfToken(),
    errors: [],
    currentUser: req.currentUser
  });
});

router.post('/edit', authenticate, [
  body('username').optional().trim().isLength({ min: 3, max: 50 }).matches(/^[a-zA-Z0-9_-]+$/),
  body('newPassword').optional({ checkFalsy: true }).isLength({ min: constants.AUTH.MIN_PASSWORD_LENGTH }),
  body('confirmPassword').optional({ checkFalsy: true })
], async (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(422).render('profile/edit', {
      title: 'Edit Profile',
      csrfToken: req.csrfToken(),
      errors: errors.array(),
      currentUser: req.currentUser
    });
  }

  try {
    const user = await User.scope('withAuth').findByPk(req.currentUser.id);
    if (!user) return next(new Error('User not found'));

    const updates = {};
    let emailChanged = false;

    // Username update
    if (req.body.username && req.body.username !== user.username) {
      updates.username = req.body.username;
    }

    // Password change - requires current password verification
    if (req.body.newPassword) {
      if (req.body.newPassword !== req.body.confirmPassword) {
        return res.status(422).render('profile/edit', {
          title: 'Edit Profile',
          csrfToken: req.csrfToken(),
          errors: [{ msg: 'New passwords do not match.' }],
          currentUser: req.currentUser
        });
      }

      const validCurrent = await authService.verifyPassword(
        req.body.currentPassword || '',
        user.passwordHash
      );

      if (!validCurrent) {
        await auditService.log('profile.password_change_failed', {
          actorId: user.id,
          outcome: 'failure',
          ip: req.ip,
          correlationId: req.correlationId
        });
        return res.status(422).render('profile/edit', {
          title: 'Edit Profile',
          csrfToken: req.csrfToken(),
          errors: [{ msg: 'Current password is incorrect.' }],
          currentUser: req.currentUser
        });
      }

      const { valid, message } = authService.validatePasswordStrength(req.body.newPassword);
      if (!valid) {
        return res.status(422).render('profile/edit', {
          title: 'Edit Profile',
          csrfToken: req.csrfToken(),
          errors: [{ msg: message }],
          currentUser: req.currentUser
        });
      }

      updates.passwordHash = await authService.hashPassword(req.body.newPassword);
      emailService.sendPasswordChangeNotification(user.email);
    }

    if (Object.keys(updates).length > 0) {
      await user.update(updates);

      await auditService.log('profile.updated', {
        actorId: user.id,
        targetId: user.id,
        targetType: 'user',
        outcome: 'success',
        ip: req.ip,
        correlationId: req.correlationId
      });
    }

    req.flash('success', 'Profile updated.');
    res.redirect('/profile/edit');
  } catch (err) {
    next(err);
  }
});

module.exports = router;
