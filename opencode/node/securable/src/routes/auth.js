const express = require('express');
const router = express.Router();
const { v4: uuidv4 } = require('uuid');
const User = require('../models/User');
const ActivityLog = require('../models/ActivityLog');
const { requireGuest, requireAuth } = require('../middleware/auth');
const { registerValidation, loginValidation, passwordResetRequestValidation, passwordResetValidation } = require('../middleware/validation');
const logger = require('../utils/logger');

router.get('/register', requireGuest, (req, res) => {
  res.render('register', { user: req.user, errors: [], values: {} });
});

router.post('/register', requireGuest, registerValidation, async (req, res) => {
  try {
    const { username, email, password } = req.body;
    
    const existingUsername = await User.findByUsername(username);
    if (existingUsername) {
      req.flash('error', 'Username already exists');
      return res.render('register', { user: null, errors: [], values: req.body });
    }
    
    const existingEmail = await User.findByEmail(email);
    if (existingEmail) {
      req.flash('error', 'Email already registered');
      return res.render('register', { user: null, errors: [], values: req.body });
    }
    
    await User.create(username, email, password);
    
    req.flash('success', 'Registration successful! Please log in.');
    res.redirect('/auth/login');
  } catch (error) {
    logger.error('Registration error', { error: error.message });
    req.flash('error', 'An error occurred during registration');
    res.render('register', { user: null, errors: [], values: req.body });
  }
});

router.get('/login', requireGuest, (req, res) => {
  res.render('login', { user: req.user });
});

router.post('/login', requireGuest, loginValidation, async (req, res) => {
  try {
    const { username, password } = req.body;
    
    const user = await User.findByUsername(username);
    if (!user) {
      await ActivityLog.log(null, 'LOGIN_FAILED', 'user', null, 'User not found', req.ip);
      req.flash('error', 'Invalid username or password');
      return res.redirect('/auth/login');
    }
    
    const isValidPassword = await User.verifyPassword(user, password);
    if (!isValidPassword) {
      await ActivityLog.log(user.id, 'LOGIN_FAILED', 'user', user.id, 'Invalid password', req.ip);
      req.flash('error', 'Invalid username or password');
      return res.redirect('/auth/login');
    }
    
    req.session.userId = user.id;
    await ActivityLog.log(user.id, 'LOGIN_SUCCESS', 'user', user.id, null, req.ip);
    logger.info('User logged in', { userId: user.id, username: user.username });
    
    req.flash('success', `Welcome back, ${user.username}!`);
    res.redirect('/notes');
  } catch (error) {
    logger.error('Login error', { error: error.message });
    req.flash('error', 'An error occurred during login');
    res.redirect('/auth/login');
  }
});

router.post('/logout', requireAuth, async (req, res) => {
  await ActivityLog.log(req.user.id, 'LOGOUT', 'user', req.user.id, null, req.ip);
  req.session.destroy((err) => {
    if (err) {
      logger.error('Logout error', { error: err.message });
    }
    res.redirect('/');
  });
});

router.get('/logout', (req, res) => {
  res.redirect('/');
});

router.get('/forgot-password', requireGuest, (req, res) => {
  res.render('forgot-password', { user: req.user });
});

router.post('/forgot-password', requireGuest, passwordResetRequestValidation, async (req, res) => {
  try {
    const { email } = req.body;
    const user = await User.findByEmail(email);
    
    if (user) {
      const token = uuidv4();
      const expiresAt = new Date(Date.now() + 3600000).toISOString();
      await User.setPasswordResetToken(email, token, expiresAt);
      
      logger.info('Password reset requested', { userId: user.id, email });
    }
    
    req.flash('success', 'If the email exists, a reset link has been sent.');
    res.redirect('/auth/login');
  } catch (error) {
    logger.error('Password reset request error', { error: error.message });
    req.flash('error', 'An error occurred');
    res.redirect('/auth/forgot-password');
  }
});

router.get('/reset-password/:token', requireGuest, async (req, res) => {
  try {
    const user = await User.findByPasswordResetToken(req.params.token);
    
    if (!user) {
      req.flash('error', 'Invalid or expired reset token');
      return res.redirect('/auth/forgot-password');
    }
    
    res.render('reset-password', { user: req.user, token: req.params.token });
  } catch (error) {
    logger.error('Password reset page error', { error: error.message });
    req.flash('error', 'An error occurred');
    res.redirect('/auth/forgot-password');
  }
});

router.post('/reset-password', requireGuest, passwordResetValidation, async (req, res) => {
  try {
    const { token, password } = req.body;
    const user = await User.findByPasswordResetToken(token);
    
    if (!user) {
      req.flash('error', 'Invalid or expired reset token');
      return res.redirect('/auth/forgot-password');
    }
    
    await User.updatePassword(user.id, password);
    await User.clearPasswordResetToken(user.id);
    await ActivityLog.log(user.id, 'PASSWORD_RESET', 'user', user.id, null, req.ip);
    
    logger.info('Password reset completed', { userId: user.id });
    req.flash('success', 'Password has been reset. Please log in with your new password.');
    res.redirect('/auth/login');
  } catch (error) {
    logger.error('Password reset error', { error: error.message });
    req.flash('error', 'An error occurred');
    res.redirect('/auth/reset-password/' + req.body.token);
  }
});

module.exports = router;
