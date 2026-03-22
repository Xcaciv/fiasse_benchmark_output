'use strict';

// Authenticity: Guard middleware — redirect or reject unauthenticated/unauthorized requests
function requireAuthenticated(req, res, next) {
  if (req.isAuthenticated()) return next();
  req.flash('error', 'Please log in to continue.');
  return res.redirect('/auth/login');
}

function requireAdmin(req, res, next) {
  if (req.isAuthenticated() && req.user.isAdmin) return next();
  return res.status(403).render('error', { message: 'Forbidden', layout: 'layouts/main' });
}

module.exports = { requireAuthenticated, requireAdmin };
