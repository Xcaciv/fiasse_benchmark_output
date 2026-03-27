'use strict';

/**
 * Requires authenticated session; stores original URL for post-login redirect.
 */
function requireAuth(req, res, next) {
  if (req.isAuthenticated()) {
    return next();
  }
  req.session.returnTo = req.originalUrl;
  req.flash('error', 'Please log in to continue.');
  res.redirect('/auth/login');
}

module.exports = requireAuth;
