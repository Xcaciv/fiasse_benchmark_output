const express = require('express');
const router = express.Router();
const User = require('../models/User');
const ActivityLog = require('../models/ActivityLog');
const { requireAuth } = require('../middleware/auth');
const { profileValidation } = require('../middleware/validation');
const logger = require('../utils/logger');

router.get('/profile', requireAuth, async (req, res) => {
  res.render('users/profile', { user: req.user });
});

router.get('/settings', requireAuth, async (req, res) => {
  res.render('users/settings', { user: req.user });
});

router.put('/profile', requireAuth, profileValidation, async (req, res) => {
  try {
    const { username, email } = req.body;
    const updates = {};
    
    if (username && username !== req.user.username) {
      const existing = await User.findByUsername(username);
      if (existing) {
        req.flash('error', 'Username already taken');
        return res.redirect('/users/settings');
      }
      updates.username = username;
    }
    
    if (email && email !== req.user.email) {
      const existing = await User.findByEmail(email);
      if (existing) {
        req.flash('error', 'Email already registered');
        return res.redirect('/users/settings');
      }
      updates.email = email;
    }
    
    if (Object.keys(updates).length > 0) {
      await User.update(req.user.id, updates);
      await ActivityLog.log(req.user.id, 'PROFILE_UPDATED', 'user', req.user.id, JSON.stringify(updates), req.ip);
      
      req.flash('success', 'Profile updated successfully');
    } else {
      req.flash('info', 'No changes made');
    }
    
    res.redirect('/users/settings');
  } catch (error) {
    logger.error('Error updating profile', { error: error.message });
    req.flash('error', 'Failed to update profile');
    res.redirect('/users/settings');
  }
});

router.put('/password', requireAuth, async (req, res) => {
  try {
    const { currentPassword, newPassword, confirmPassword } = req.body;
    
    if (newPassword !== confirmPassword) {
      req.flash('error', 'New passwords do not match');
      return res.redirect('/users/settings');
    }
    
    if (newPassword.length < 8) {
      req.flash('error', 'Password must be at least 8 characters');
      return res.redirect('/users/settings');
    }
    
    const user = await User.findById(req.user.id);
    const isValid = await User.verifyPassword(user, currentPassword);
    
    if (!isValid) {
      req.flash('error', 'Current password is incorrect');
      return res.redirect('/users/settings');
    }
    
    await User.updatePassword(req.user.id, newPassword);
    await ActivityLog.log(req.user.id, 'PASSWORD_CHANGED', 'user', req.user.id, null, req.ip);
    
    req.flash('success', 'Password changed successfully');
    res.redirect('/users/settings');
  } catch (error) {
    logger.error('Error changing password', { error: error.message });
    req.flash('error', 'Failed to change password');
    res.redirect('/users/settings');
  }
});

module.exports = router;
