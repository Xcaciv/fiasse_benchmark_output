const express = require('express');
const bcrypt = require('bcrypt');
const { v4: uuidv4 } = require('uuid');
const { body, validationResult } = require('express-validator');
const { db } = require('../db/database');
const { isGuest, isAuthenticated } = require('../middleware/auth');

const router = express.Router();

router.get('/register', isGuest, (req, res) => {
  res.render('auth/register', { 
    user: null, 
    errors: [],
    formData: {} 
  });
});

router.post('/register', isGuest, [
  body('username').trim().isLength({ min: 3, max: 30 }).withMessage('Username must be between 3 and 30 characters'),
  body('email').isEmail().withMessage('Please enter a valid email'),
  body('password').isLength({ min: 6 }).withMessage('Password must be at least 6 characters'),
  body('confirmPassword').custom((value, { req }) => {
    if (value !== req.body.password) {
      throw new Error('Passwords do not match');
    }
    return true;
  })
], async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('auth/register', { 
      user: null, 
      errors: errors.array(),
      formData: req.body
    });
  }

  const { username, email, password } = req.body;

  try {
    const existingUser = db.prepare('SELECT id FROM users WHERE username = ? OR email = ?').get(username, email);
    if (existingUser) {
      return res.render('auth/register', { 
        user: null, 
        errors: [{ msg: 'Username or email already exists' }],
        formData: req.body
      });
    }

    const passwordHash = await bcrypt.hash(password, 10);
    
    const result = db.prepare(
      'INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)'
    ).run(username, email.toLowerCase(), passwordHash);

    db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
      result.lastInsertRowid,
      'user_registered',
      `User ${username} registered`
    );

    req.flash('success', 'Registration successful! Please log in.');
    res.redirect('/auth/login');
  } catch (error) {
    console.error('Registration error:', error);
    res.render('auth/register', { 
      user: null, 
      errors: [{ msg: 'An error occurred during registration' }],
      formData: req.body
    });
  }
});

router.get('/login', isGuest, (req, res) => {
  res.render('auth/login', { 
    user: null, 
    errors: [],
    formData: {} 
  });
});

router.post('/login', isGuest, [
  body('username').trim().notEmpty().withMessage('Username is required'),
  body('password').notEmpty().withMessage('Password is required')
], async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('auth/login', { 
      user: null, 
      errors: errors.array(),
      formData: req.body
    });
  }

  const { username, password } = req.body;

  try {
    const user = db.prepare('SELECT * FROM users WHERE username = ?').get(username);
    
    if (!user) {
      db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
        null,
        'login_failed',
        `Failed login attempt for username: ${username}`
      );
      return res.render('auth/login', { 
        user: null, 
        errors: [{ msg: 'Invalid username or password' }],
        formData: req.body
      });
    }

    const isMatch = await bcrypt.compare(password, user.password_hash);
    
    if (!isMatch) {
      db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
        user.id,
        'login_failed',
        `Failed login attempt for user: ${username}`
      );
      return res.render('auth/login', { 
        user: null, 
        errors: [{ msg: 'Invalid username or password' }],
        formData: req.body
      });
    }

    req.session.user = {
      id: user.id,
      username: user.username,
      email: user.email,
      role: user.role
    };

    db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
      user.id,
      'login_success',
      `User ${username} logged in`
    );

    req.flash('success', `Welcome back, ${user.username}!`);
    res.redirect('/');
  } catch (error) {
    console.error('Login error:', error);
    res.render('auth/login', { 
      user: null, 
      errors: [{ msg: 'An error occurred during login' }],
      formData: req.body
    });
  }
});

router.post('/logout', isAuthenticated, (req, res) => {
  const username = req.session.user.username;
  
  db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
    req.session.user.id,
    'logout',
    `User ${username} logged out`
  );

  req.session.destroy((err) => {
    if (err) {
      console.error('Logout error:', err);
    }
    res.redirect('/');
  });
});

router.get('/forgot-password', isGuest, (req, res) => {
  res.render('auth/forgot-password', { 
    user: null, 
    errors: [],
    formData: {},
    success: null
  });
});

router.post('/forgot-password', isGuest, [
  body('email').isEmail().withMessage('Please enter a valid email')
], (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('auth/forgot-password', { 
      user: null, 
      errors: errors.array(),
      formData: req.body,
      success: null
    });
  }

  const { email } = req.body;
  const user = db.prepare('SELECT id, email FROM users WHERE email = ?').get(email.toLowerCase());

  if (!user) {
    req.flash('success', 'If the email exists, a reset link has been sent.');
    return res.redirect('/auth/forgot-password');
  }

  const token = uuidv4();
  const expiresAt = new Date(Date.now() + 60 * 60 * 1000).toISOString();

  db.prepare('INSERT INTO password_resets (user_id, token, expires_at) VALUES (?, ?, ?)').run(
    user.id,
    token,
    expiresAt
  );

  db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
    user.id,
    'password_reset_requested',
    `Password reset requested for email: ${email}`
  );

  req.flash('success', 'Password reset link generated. In a real application, this would be sent via email.');
  req.flash('resetToken', token);
  req.flash('resetEmail', email);
  
  res.redirect('/auth/forgot-password');
});

router.get('/reset-password/:token', isGuest, (req, res) => {
  const { token } = req.params;
  
  const resetRecord = db.prepare(`
    SELECT * FROM password_resets 
    WHERE token = ? AND used = 0 AND expires_at > datetime('now')
  `).get(token);

  if (!resetRecord) {
    req.flash('error', 'Invalid or expired reset token');
    return res.redirect('/auth/forgot-password');
  }

  res.render('auth/reset-password', { 
    user: null, 
    errors: [],
    formData: {},
    token
  });
});

router.post('/reset-password/:token', isGuest, [
  body('password').isLength({ min: 6 }).withMessage('Password must be at least 6 characters'),
  body('confirmPassword').custom((value, { req }) => {
    if (value !== req.body.password) {
      throw new Error('Passwords do not match');
    }
    return true;
  })
], async (req, res) => {
  const { token } = req.params;
  const errors = validationResult(req);
  
  if (!errors.isEmpty()) {
    return res.render('auth/reset-password', { 
      user: null, 
      errors: errors.array(),
      formData: req.body,
      token
    });
  }

  const resetRecord = db.prepare(`
    SELECT * FROM password_resets 
    WHERE token = ? AND used = 0 AND expires_at > datetime('now')
  `).get(token);

  if (!resetRecord) {
    req.flash('error', 'Invalid or expired reset token');
    return res.redirect('/auth/forgot-password');
  }

  try {
    const passwordHash = await bcrypt.hash(req.body.password, 10);
    
    db.prepare('UPDATE users SET password_hash = ?, updated_at = datetime(\'now\') WHERE id = ?').run(
      passwordHash,
      resetRecord.user_id
    );

    db.prepare('UPDATE password_resets SET used = 1 WHERE id = ?').run(resetRecord.id);

    db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
      resetRecord.user_id,
      'password_reset_completed',
      'Password has been reset'
    );

    req.flash('success', 'Password has been reset successfully. Please log in.');
    res.redirect('/auth/login');
  } catch (error) {
    console.error('Password reset error:', error);
    res.render('auth/reset-password', { 
      user: null, 
      errors: [{ msg: 'An error occurred while resetting password' }],
      formData: req.body,
      token
    });
  }
});

router.get('/profile', isAuthenticated, (req, res) => {
  res.render('auth/profile', { 
    user: req.session.user,
    errors: [],
    formData: req.session.user,
    success: null
  });
});

router.post('/profile', isAuthenticated, [
  body('username').trim().isLength({ min: 3, max: 30 }).withMessage('Username must be between 3 and 30 characters'),
  body('email').isEmail().withMessage('Please enter a valid email')
], async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('auth/profile', { 
      user: req.session.user,
      errors: errors.array(),
      formData: req.body,
      success: null
    });
  }

  const { username, email } = req.body;
  const userId = req.session.user.id;

  try {
    const existingUser = db.prepare('SELECT id FROM users WHERE (username = ? OR email = ?) AND id != ?').get(username, email.toLowerCase(), userId);
    
    if (existingUser) {
      return res.render('auth/profile', { 
        user: req.session.user,
        errors: [{ msg: 'Username or email already exists' }],
        formData: req.body,
        success: null
      });
    }

    db.prepare('UPDATE users SET username = ?, email = ?, updated_at = datetime(\'now\') WHERE id = ?').run(
      username,
      email.toLowerCase(),
      userId
    );

    db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
      userId,
      'profile_updated',
      `User updated profile: username=${username}, email=${email}`
    );

    req.session.user.username = username;
    req.session.user.email = email.toLowerCase();

    req.flash('success', 'Profile updated successfully');
    res.redirect('/auth/profile');
  } catch (error) {
    console.error('Profile update error:', error);
    res.render('auth/profile', { 
      user: req.session.user,
      errors: [{ msg: 'An error occurred while updating profile' }],
      formData: req.body,
      success: null
    });
  }
});

router.post('/change-password', isAuthenticated, [
  body('currentPassword').notEmpty().withMessage('Current password is required'),
  body('newPassword').isLength({ min: 6 }).withMessage('New password must be at least 6 characters'),
  body('confirmNewPassword').custom((value, { req }) => {
    if (value !== req.body.newPassword) {
      throw new Error('Passwords do not match');
    }
    return true;
  })
], async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('auth/profile', { 
      user: req.session.user,
      errors: errors.array(),
      formData: req.session.user,
      success: null
    });
  }

  const { currentPassword, newPassword } = req.body;
  const userId = req.session.user.id;

  try {
    const user = db.prepare('SELECT password_hash FROM users WHERE id = ?').get(userId);
    
    const isMatch = await bcrypt.compare(currentPassword, user.password_hash);
    
    if (!isMatch) {
      return res.render('auth/profile', { 
        user: req.session.user,
        errors: [{ msg: 'Current password is incorrect' }],
        formData: req.session.user,
        success: null
      });
    }

    const passwordHash = await bcrypt.hash(newPassword, 10);
    
    db.prepare('UPDATE users SET password_hash = ?, updated_at = datetime(\'now\') WHERE id = ?').run(
      passwordHash,
      userId
    );

    db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
      userId,
      'password_changed',
      'User changed their password'
    );

    req.flash('success', 'Password changed successfully');
    res.redirect('/auth/profile');
  } catch (error) {
    console.error('Password change error:', error);
    res.render('auth/profile', { 
      user: req.session.user,
      errors: [{ msg: 'An error occurred while changing password' }],
      formData: req.session.user,
      success: null
    });
  }
});

module.exports = router;
