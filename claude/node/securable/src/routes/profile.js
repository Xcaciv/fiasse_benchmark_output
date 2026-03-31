'use strict';

const express = require('express');
const { body } = require('express-validator');
const { User } = require('../models');
const authService = require('../services/authService');
const auditService = require('../services/auditService');
const { handleValidationErrors } = require('../middleware/validate');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

router.use(requireAuth);

// GET /profile
router.get('/', (req, res) => {
  res.render('profile/edit', { title: 'Edit Profile', user: req.user });
});

// POST /profile/update
router.post(
  '/update',
  [
    body('username')
      .trim()
      .isLength({ min: 3, max: 64 })
      .withMessage('Username must be 3–64 characters.')
      .matches(/^[a-z0-9_.-]+$/i)
      .withMessage('Username may only contain letters, numbers, underscores, hyphens, and dots.'),
    body('email').trim().isEmail().normalizeEmail().withMessage('Valid email is required.'),
  ],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      // Request Surface Minimization: extract only expected fields
      const { username, email } = req.body;
      const canonicalUsername = username.trim().toLowerCase();
      const canonicalEmail = email.trim().toLowerCase();

      // Check uniqueness excluding current user
      const { Op } = require('sequelize');
      const conflictUser = await User.findOne({
        where: {
          [Op.or]: [{ username: canonicalUsername }, { email: canonicalEmail }],
          id: { [Op.ne]: req.user.id },
        },
        attributes: ['id', 'username', 'email'],
      });

      if (conflictUser) {
        req.flash('error', 'Username or email already in use.');
        return res.redirect('/profile');
      }

      await User.update(
        { username: canonicalUsername, email: canonicalEmail },
        { where: { id: req.user.id } }
      );

      await auditService.record({ userId: req.user.id, action: 'PROFILE_UPDATE' });
      req.flash('success', 'Profile updated.');
      res.redirect('/profile');
    } catch (err) {
      next(err);
    }
  }
);

// POST /profile/change-password
router.post(
  '/change-password',
  [
    body('currentPassword').notEmpty().withMessage('Current password is required.'),
    body('newPassword')
      .isLength({ min: 8 })
      .withMessage('New password must be at least 8 characters.')
      .matches(/[A-Z]/)
      .withMessage('New password must contain at least one uppercase letter.')
      .matches(/[0-9]/)
      .withMessage('New password must contain at least one number.'),
    body('confirmPassword').custom((value, { req }) => {
      if (value !== req.body.newPassword) throw new Error('Passwords do not match.');
      return true;
    }),
  ],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      const { currentPassword, newPassword } = req.body;
      await authService.changePassword(req.user.id, currentPassword, newPassword);
      req.flash('success', 'Password changed successfully.');
      res.redirect('/profile');
    } catch (err) {
      if (err.status === 400) {
        req.flash('error', err.message);
        return res.redirect('/profile');
      }
      next(err);
    }
  }
);

module.exports = router;
