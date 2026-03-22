'use strict';

/** Attach user info to res.locals on every request. */
function loadUser(req, res, next) {
  if (req.session && req.session.userId) {
    res.locals.currentUser = {
      id: req.session.userId,
      username: req.session.username,
      role: req.session.userRole
    };
  } else {
    res.locals.currentUser = null;
  }
  next();
}

/** Redirect unauthenticated users to /auth/login. */
function requireAuth(req, res, next) {
  if (!req.session || !req.session.userId) {
    req.session.returnTo = req.originalUrl;
    return res.redirect('/auth/login');
  }
  next();
}

/** Require the 'admin' role. */
function requireAdmin(req, res, next) {
  if (!req.session || !req.session.userId) {
    return res.redirect('/auth/login');
  }
  if (req.session.userRole !== 'admin') {
    return res.status(403).render('error', {
      title: 'Forbidden',
      message: 'Administrator access is required.'
    });
  }
  next();
}

module.exports = { loadUser, requireAuth, requireAdmin };
