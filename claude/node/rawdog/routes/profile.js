const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const { body, validationResult } = require('express-validator');
const db = require('../models');
const { isAuthenticated } = require('../middleware/auth');

// GET /profile/edit
router.get('/edit', isAuthenticated, (req, res) => {
  res.render('profile/edit', { title: 'Edit Profile', errors: [] });
});

// POST /profile/edit
router.post('/edit', isAuthenticated, [
  body('username').trim().isLength({ min: 3, max: 50 }).withMessage('Username must be 3-50 characters.'),
  body('email').trim().isEmail().withMessage('Please provide a valid email address.').normalizeEmail(),
  body('newPassword').optional({ checkFalsy: true }).isLength({ min: 6 }).withMessage('New password must be at least 6 characters.'),
  body('confirmPassword').custom((value, { req }) => {
    if (req.body.newPassword && value !== req.body.newPassword) {
      throw new Error('Passwords do not match.');
    }
    return true;
  })
], async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.render('profile/edit', { title: 'Edit Profile', errors: errors.array() });
    }

    const { username, email, currentPassword, newPassword } = req.body;

    const existingUsername = await db.User.findOne({ where: { username } });
    if (existingUsername && existingUsername.id !== req.user.id) {
      return res.render('profile/edit', {
        title: 'Edit Profile',
        errors: [{ msg: 'Username already taken.' }]
      });
    }

    const existingEmail = await db.User.findOne({ where: { email } });
    if (existingEmail && existingEmail.id !== req.user.id) {
      return res.render('profile/edit', {
        title: 'Edit Profile',
        errors: [{ msg: 'Email already registered.' }]
      });
    }

    const updateData = { username, email };

    if (newPassword) {
      if (!currentPassword) {
        return res.render('profile/edit', {
          title: 'Edit Profile',
          errors: [{ msg: 'Current password is required to set a new password.' }]
        });
      }
      const isMatch = await bcrypt.compare(currentPassword, req.user.password);
      if (!isMatch) {
        return res.render('profile/edit', {
          title: 'Edit Profile',
          errors: [{ msg: 'Current password is incorrect.' }]
        });
      }
      updateData.password = await bcrypt.hash(newPassword, 12);
    }

    await db.User.update(updateData, { where: { id: req.user.id } });

    await db.AuditLog.create({
      userId: req.user.id,
      action: 'profile_update',
      details: `User ${req.user.username} updated their profile`,
      ipAddress: req.ip
    });

    req.flash('success_msg', 'Profile updated successfully.');
    res.redirect('/profile/edit');
  } catch (err) {
    next(err);
  }
});

module.exports = router;
