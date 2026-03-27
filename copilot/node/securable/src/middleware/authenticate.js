'use strict';

const { User } = require('../models/index');

/**
 * Redirect unauthenticated users to login.
 * Stores the requested URL so login can redirect back.
 */
const requireAuth = (req, res, next) => {
  if (!req.session || !req.session.userId) {
    req.session.returnTo = req.originalUrl;
    return res.redirect('/auth/login');
  }
  next();
};

/**
 * Allow only users with the admin role.
 * Returns 403 for authenticated non-admins.
 */
const requireAdmin = (req, res, next) => {
  if (!req.session || req.session.role !== 'admin') {
    return res.status(403).render('errors/403', {
      title: 'Access Denied',
      message: 'Administrator access required.',
      user: res.locals.user || null,
    });
  }
  next();
};

/**
 * Set req.user if a session exists but continue regardless.
 * Used for routes accessible to both authenticated and anonymous users.
 */
const optionalAuth = async (req, res, next) => {
  if (req.session && req.session.userId) {
    try {
      req.user = await User.findByPk(req.session.userId, {
        attributes: ['id', 'username', 'email', 'role'],
      });
    } catch {
      req.user = null;
    }
  } else {
    req.user = null;
  }
  next();
};

/**
 * Global middleware: resolve the logged-in user on every request and
 * expose it on res.locals for EJS templates.
 */
const loadUser = async (req, res, next) => {
  res.locals.user = null;
  res.locals.isAdmin = false;

  if (req.session && req.session.userId) {
    try {
      const user = await User.findByPk(req.session.userId, {
        attributes: ['id', 'username', 'email', 'role'],
      });
      req.user = user;
      res.locals.user = user;
      res.locals.isAdmin = user ? user.role === 'admin' : false;
    } catch {
      req.user = null;
    }
  }

  next();
};

module.exports = { requireAuth, requireAdmin, optionalAuth, loadUser };
