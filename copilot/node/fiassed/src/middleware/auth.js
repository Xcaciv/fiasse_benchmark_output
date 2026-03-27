'use strict';

function requireAuth(req, res, next) {
  if (!req.session || !req.session.userId) {
    req.session.flash = { error: 'You must be logged in to access this page' };
    return res.redirect('/login');
  }
  next();
}

function requireAdmin(req, res, next) {
  if (!req.session || !req.session.userId) {
    req.session.flash = { error: 'You must be logged in to access this page' };
    return res.redirect('/login');
  }
  if (req.session.role !== 'admin') {
    return res.status(403).render('errors/500', { message: 'Access denied' });
  }
  next();
}

function requireReAuth(req, res, next) {
  if (!req.session.reAuthVerified) {
    req.session.returnTo = req.originalUrl;
    return res.redirect('/admin/reauth');
  }
  next();
}

module.exports = { requireAuth, requireAdmin, requireReAuth };
