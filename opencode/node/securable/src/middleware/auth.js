const User = require('../models/User');

async function loadUser(req, res, next) {
  if (req.session && req.session.userId) {
    try {
      const user = await User.findById(req.session.userId);
      if (user) {
        req.user = {
          id: user.id,
          username: user.username,
          email: user.email,
          role: user.role,
        };
      } else {
        delete req.session.userId;
      }
    } catch (error) {
      delete req.session.userId;
    }
  }
  next();
}

function requireAuth(req, res, next) {
  if (!req.user) {
    req.flash('error', 'Please log in to access this page');
    return res.redirect('/auth/login');
  }
  next();
}

function requireGuest(req, res, next) {
  if (req.user) {
    return res.redirect('/notes');
  }
  next();
}

function requireAdmin(req, res, next) {
  if (!req.user) {
    req.flash('error', 'Please log in to access this page');
    return res.redirect('/auth/login');
  }
  if (req.user.role !== 'admin') {
    req.flash('error', 'You do not have permission to access this page');
    return res.redirect('/notes');
  }
  next();
}

module.exports = {
  loadUser,
  requireAuth,
  requireGuest,
  requireAdmin,
};
