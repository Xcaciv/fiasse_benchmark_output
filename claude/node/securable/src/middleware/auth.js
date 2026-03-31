'use strict';

/**
 * Authentication and authorisation guards.
 * SSEM Authenticity: every route that requires login is protected here.
 * SSEM Accountability: denial events are logged before redirecting.
 */

const logger = require('../config/logger');

function requireAuth(req, res, next) {
  if (req.isAuthenticated()) {
    return next();
  }
  logger.audit('AUTHZ_DENY', {
    reason: 'NOT_AUTHENTICATED',
    path: req.path,
    ip: req.ip,
  });
  req.flash('error', 'You must be logged in to access this page.');
  res.redirect('/auth/login');
}

function requireAdmin(req, res, next) {
  if (req.isAuthenticated() && req.user.role === 'admin') {
    return next();
  }
  logger.audit('AUTHZ_DENY', {
    reason: 'NOT_ADMIN',
    userId: req.user?.id,
    path: req.path,
    ip: req.ip,
  });
  req.flash('error', 'You do not have permission to access this page.');
  res.redirect('/notes');
}

function redirectIfAuthenticated(req, res, next) {
  if (req.isAuthenticated()) {
    return res.redirect('/notes');
  }
  next();
}

module.exports = { requireAuth, requireAdmin, redirectIfAuthenticated };
