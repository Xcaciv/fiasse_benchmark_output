'use strict';

// [TRUST BOUNDARY] Authorization middleware — Authenticity + Integrity
// All route-level access decisions flow through these helpers

function requireAuth(req, res, next) {
  if (req.isAuthenticated()) {
    return next();
  }
  req.flash('error', 'Please log in to access that page.');
  return res.redirect('/auth/login');
}

function requireAdmin(req, res, next) {
  if (req.isAuthenticated() && req.user.role === 'admin') {
    return next();
  }
  return res.status(403).render('error', {
    title: 'Access Denied',
    message: 'You do not have permission to access this resource.',
    statusCode: 403,
  });
}

function requireGuest(req, res, next) {
  if (!req.isAuthenticated()) {
    return next();
  }
  return res.redirect('/notes');
}

module.exports = { requireAuth, requireAdmin, requireGuest };
