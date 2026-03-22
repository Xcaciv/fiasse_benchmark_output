'use strict';

const { getDb } = require('../database');
const { addFlash } = require('../utils/flash');

function attachFlash(req, res, next) {
  if (!req.session.flashMessages) {
    req.session.flashMessages = [];
  }

  next();
}

function consumeFlash(req) {
  const messages = req.session.flashMessages || [];
  req.session.flashMessages = [];
  return messages;
}

function attachUser(req, res, next) {
  const userId = req.session.userId;
  if (!userId) {
    req.currentUser = null;
    return next();
  }

  const db = getDb();
  const user = db
    .prepare('SELECT id, username, email, role, created_at, updated_at FROM users WHERE id = ?')
    .get(userId);

  if (!user) {
    req.session.userId = null;
    req.currentUser = null;
  } else {
    req.currentUser = user;
  }

  return next();
}

function requireAuth(req, res, next) {
  if (!req.currentUser) {
    addFlash(req, 'warning', 'Please sign in to continue.');
    return res.redirect('/login');
  }

  return next();
}

function requireAdmin(req, res, next) {
  if (!req.currentUser || req.currentUser.role !== 'admin') {
    addFlash(req, 'danger', 'Administrator access is required for that page.');
    return res.redirect('/');
  }

  return next();
}

module.exports = {
  attachFlash,
  attachUser,
  consumeFlash,
  requireAdmin,
  requireAuth
};
