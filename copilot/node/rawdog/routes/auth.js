const express = require('express');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');
const nodemailer = require('nodemailer');
const { User, ActivityLog } = require('../models');

const router = express.Router();

// GET /auth/register
router.get('/register', (req, res) => {
  res.render('auth/register', { title: 'Register' });
});

// POST /auth/register
router.post('/register', async (req, res) => {
  try {
    const { username, email, password, confirmPassword } = req.body;
    if (!username || !email || !password) {
      req.flash('error', 'All fields are required.');
      return res.redirect('/auth/register');
    }
    if (password !== confirmPassword) {
      req.flash('error', 'Passwords do not match.');
      return res.redirect('/auth/register');
    }
    if (password.length < 6) {
      req.flash('error', 'Password must be at least 6 characters.');
      return res.redirect('/auth/register');
    }
    const existing = await User.findOne({ where: { username } });
    if (existing) {
      req.flash('error', 'Username already taken.');
      return res.redirect('/auth/register');
    }
    const existingEmail = await User.findOne({ where: { email } });
    if (existingEmail) {
      req.flash('error', 'Email already registered.');
      return res.redirect('/auth/register');
    }
    const hashed = await bcrypt.hash(password, 12);
    await User.create({ username, email, password: hashed });
    req.flash('success', 'Registration successful! Please log in.');
    res.redirect('/auth/login');
  } catch (err) {
    console.error(err);
    req.flash('error', 'Registration failed.');
    res.redirect('/auth/register');
  }
});

// GET /auth/login
router.get('/login', (req, res) => {
  res.render('auth/login', { title: 'Login' });
});

// POST /auth/login
router.post('/login', async (req, res) => {
  try {
    const { username, password } = req.body;
    const user = await User.findOne({ where: { username } });
    if (!user) {
      req.flash('error', 'Invalid username or password.');
      return res.redirect('/auth/login');
    }
    const valid = await bcrypt.compare(password, user.password);
    if (!valid) {
      req.flash('error', 'Invalid username or password.');
      return res.redirect('/auth/login');
    }
    req.session.userId = user.id;
    req.session.role = user.role;
    req.session.username = user.username;
    await ActivityLog.create({ userId: user.id, action: 'login', details: `User ${user.username} logged in` });
    req.flash('success', `Welcome back, ${user.username}!`);
    res.redirect('/notes');
  } catch (err) {
    console.error(err);
    req.flash('error', 'Login failed.');
    res.redirect('/auth/login');
  }
});

// POST /auth/logout
router.post('/logout', async (req, res) => {
  try {
    const userId = req.session.userId;
    const username = req.session.username;
    req.session.destroy(async () => {
      if (userId) {
        await ActivityLog.create({ userId, action: 'logout', details: `User ${username} logged out` });
      }
      res.redirect('/auth/login');
    });
  } catch (err) {
    console.error(err);
    res.redirect('/');
  }
});

// GET /auth/forgot-password
router.get('/forgot-password', (req, res) => {
  res.render('auth/forgot-password', { title: 'Forgot Password' });
});

// POST /auth/forgot-password
router.post('/forgot-password', async (req, res) => {
  try {
    const { email } = req.body;
    const user = await User.findOne({ where: { email } });
    if (!user) {
      req.flash('info', 'If that email is registered, a reset link has been sent.');
      return res.redirect('/auth/forgot-password');
    }
    const token = uuidv4();
    const expires = new Date(Date.now() + 3600000); // 1 hour
    await user.update({ passwordResetToken: token, passwordResetExpires: expires });

    const baseUrl = process.env.BASE_URL || 'http://localhost:3000';
    const resetUrl = `${baseUrl}/auth/reset-password?token=${token}`;

    const transporter = nodemailer.createTransport({
      host: process.env.EMAIL_HOST || 'smtp.ethereal.email',
      port: parseInt(process.env.EMAIL_PORT || '587'),
      auth: {
        user: process.env.EMAIL_USER,
        pass: process.env.EMAIL_PASS,
      },
    });

    await transporter.sendMail({
      from: '"Loose Notes" <noreply@loosenotes.app>',
      to: user.email,
      subject: 'Password Reset',
      text: `Click the link to reset your password: ${resetUrl}\n\nExpires in 1 hour.`,
      html: `<p>Click <a href="${resetUrl}">here</a> to reset your password.</p><p>Expires in 1 hour.</p>`,
    }).catch(err => console.error('Email error:', err.message));

    req.flash('info', 'If that email is registered, a reset link has been sent.');
    res.redirect('/auth/forgot-password');
  } catch (err) {
    console.error(err);
    req.flash('error', 'Something went wrong.');
    res.redirect('/auth/forgot-password');
  }
});

// GET /auth/reset-password
router.get('/reset-password', async (req, res) => {
  try {
    const { token } = req.query;
    const user = await User.findOne({ where: { passwordResetToken: token } });
    if (!user || !user.passwordResetExpires || user.passwordResetExpires < new Date()) {
      req.flash('error', 'Invalid or expired reset token.');
      return res.redirect('/auth/forgot-password');
    }
    res.render('auth/reset-password', { title: 'Reset Password', token });
  } catch (err) {
    console.error(err);
    res.redirect('/auth/forgot-password');
  }
});

// POST /auth/reset-password
router.post('/reset-password', async (req, res) => {
  try {
    const { token, password, confirmPassword } = req.body;
    if (password !== confirmPassword) {
      req.flash('error', 'Passwords do not match.');
      return res.redirect(`/auth/reset-password?token=${token}`);
    }
    if (password.length < 6) {
      req.flash('error', 'Password must be at least 6 characters.');
      return res.redirect(`/auth/reset-password?token=${token}`);
    }
    const user = await User.findOne({ where: { passwordResetToken: token } });
    if (!user || !user.passwordResetExpires || user.passwordResetExpires < new Date()) {
      req.flash('error', 'Invalid or expired reset token.');
      return res.redirect('/auth/forgot-password');
    }
    const hashed = await bcrypt.hash(password, 12);
    await user.update({ password: hashed, passwordResetToken: null, passwordResetExpires: null });
    req.flash('success', 'Password reset successfully. Please log in.');
    res.redirect('/auth/login');
  } catch (err) {
    console.error(err);
    req.flash('error', 'Reset failed.');
    res.redirect('/auth/forgot-password');
  }
});

module.exports = router;
