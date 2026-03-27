'use strict';

const bcrypt = require('bcrypt');
const passport = require('passport');
const { validationResult } = require('express-validator');
const { User } = require('../models');
const { generateToken, hashToken } = require('../utils/tokenUtils');
const { sendPasswordResetEmail } = require('../services/emailService');
const { logActivity } = require('../services/auditService');
const logger = require('../utils/logger');

const BCRYPT_ROUNDS = 12;

function getRegisterPage(req, res) {
  res.render('auth/register', { title: 'Register', csrfToken: req.csrfToken() });
}

async function postRegister(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    req.flash('error', errors.array().map((e) => e.msg).join(' '));
    return res.redirect('/auth/register');
  }
  try {
    const { username, email, password } = req.body;
    const exists = await User.scope('withAuth').findOne({
      where: { username },
    });
    if (exists) {
      req.flash('error', 'That username is already taken.');
      return res.redirect('/auth/register');
    }
    const passwordHash = await bcrypt.hash(password, BCRYPT_ROUNDS);
    await User.create({ username, email: email.toLowerCase(), passwordHash });
    req.flash('success', 'Account created! Please log in.');
    return res.redirect('/auth/login');
  } catch (err) {
    if (err.name === 'SequelizeUniqueConstraintError') {
      req.flash('error', 'Registration failed. Please try again.');
      return res.redirect('/auth/register');
    }
    return next(err);
  }
}

function getLoginPage(req, res) {
  res.render('auth/login', { title: 'Login', csrfToken: req.csrfToken() });
}

function postLogin(req, res, next) {
  passport.authenticate('local', (err, user, info) => {
    if (err) return next(err);
    if (!user) {
      req.flash('error', info ? info.message : 'Invalid credentials.');
      return res.redirect('/auth/login');
    }
    // Regenerate session to prevent session fixation
    req.session.regenerate((sessionErr) => {
      if (sessionErr) return next(sessionErr);
      req.logIn(user, (loginErr) => {
        if (loginErr) return next(loginErr);
        const returnTo = req.session.returnTo || '/notes';
        delete req.session.returnTo;
        return res.redirect(returnTo);
      });
    });
  })(req, res, next);
}

function postLogout(req, res, next) {
  const userId = req.user ? req.user.id : null;
  req.session.destroy((err) => {
    if (err) return next(err);
    if (userId) {
      logActivity({ userId, action: 'user.logout' }).catch(() => {});
    }
    res.clearCookie('connect.sid');
    return res.redirect('/');
  });
}

function getForgotPasswordPage(req, res) {
  res.render('auth/forgotPassword', { title: 'Forgot Password', csrfToken: req.csrfToken() });
}

async function postForgotPassword(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    req.flash('error', 'Please provide a valid email address.');
    return res.redirect('/auth/forgot-password');
  }
  try {
    const { email } = req.body;
    // Always show same message to prevent email enumeration
    const successMsg = 'If that email is registered, you will receive a reset link shortly.';
    const user = await User.scope('withAuth').findOne({ where: { email: email.toLowerCase() } });
    if (!user) {
      req.flash('info', successMsg);
      return res.redirect('/auth/forgot-password');
    }
    const rawToken = generateToken();
    const hashedToken = hashToken(rawToken);
    const expires = new Date(Date.now() + 60 * 60 * 1000);
    await user.update({ passwordResetToken: hashedToken, passwordResetExpires: expires });
    const resetUrl = `${process.env.BASE_URL || 'http://localhost:3000'}/auth/reset-password/${rawToken}`;
    await sendPasswordResetEmail(user.email, resetUrl);
    req.flash('info', successMsg);
    return res.redirect('/auth/forgot-password');
  } catch (err) {
    logger.error('Forgot password error', { error: err.message });
    req.flash('error', 'An error occurred. Please try again.');
    return res.redirect('/auth/forgot-password');
  }
}

function getResetPasswordPage(req, res) {
  const { token } = req.params;
  res.render('auth/resetPassword', { title: 'Reset Password', token, csrfToken: req.csrfToken() });
}

async function postResetPassword(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    req.flash('error', errors.array().map((e) => e.msg).join(' '));
    return res.redirect(`/auth/reset-password/${req.params.token}`);
  }
  try {
    const { token, password } = req.body;
    const hashedToken = hashToken(token);
    const user = await User.scope('withAuth').findOne({
      where: { passwordResetToken: hashedToken },
    });
    if (!user || !user.passwordResetExpires || user.passwordResetExpires < new Date()) {
      req.flash('error', 'Reset link is invalid or has expired.');
      return res.redirect('/auth/forgot-password');
    }
    const passwordHash = await bcrypt.hash(password, BCRYPT_ROUNDS);
    await user.update({ passwordHash, passwordResetToken: null, passwordResetExpires: null });
    await logActivity({ userId: user.id, action: 'user.password.reset' });
    req.flash('success', 'Password reset successfully. Please log in.');
    return res.redirect('/auth/login');
  } catch (err) {
    return next(err);
  }
}

module.exports = {
  getRegisterPage,
  postRegister,
  getLoginPage,
  postLogin,
  postLogout,
  getForgotPasswordPage,
  postForgotPassword,
  getResetPasswordPage,
  postResetPassword,
};
