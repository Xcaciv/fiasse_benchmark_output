const { getDb } = require('../db');
const { getAuthenticatedUserId, redirectWithSession } = require('../lib/auth');

async function attachCurrentUser(req, res, next) {
  const authenticatedUserId = getAuthenticatedUserId(req);

  if (!authenticatedUserId) {
    req.currentUser = null;
    return next();
  }

  const db = await getDb();
  const user = await db.get(
    `
      SELECT id, username, email, role, created_at, updated_at
      FROM users
      WHERE id = ?
    `,
    [authenticatedUserId]
  );

  req.currentUser = user || null;
  next();
}

async function requireAuth(req, res, next) {
  if (req.currentUser) {
    next();
    return;
  }

  req.flash('error', 'Please log in to continue.');
  await redirectWithSession(req, res, '/auth/login');
}

async function requireAdmin(req, res, next) {
  if (req.currentUser && req.currentUser.role === 'Admin') {
    next();
    return;
  }

  req.flash('error', 'Administrator access is required.');
  await redirectWithSession(req, res, '/');
}

function redirectIfAuthenticated(req, res, next) {
  if (req.currentUser) {
    res.redirect('/');
    return;
  }

  next();
}

module.exports = {
  attachCurrentUser,
  requireAuth,
  requireAdmin,
  redirectIfAuthenticated
};
