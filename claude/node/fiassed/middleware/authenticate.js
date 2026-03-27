'use strict';

const { User } = require('../models');
const { logger } = require('../config/logger');
const constants = require('../config/constants');

/**
 * Authenticate middleware: verify session is valid and user still exists in DB.
 * Authenticity: session user state is re-verified from DB on each request,
 * not trusted from cached session alone.
 */
async function authenticate(req, res, next) {
  if (!req.session || !req.session.userId) {
    return redirectOrUnauthorized(req, res);
  }

  try {
    const user = await User.findByPk(req.session.userId, {
      attributes: ['id', 'username', 'email', 'role', 'isActive']
    });

    if (!user || !user.isActive) {
      req.session.destroy(() => {});
      return redirectOrUnauthorized(req, res);
    }

    // Attach fresh DB record to request - do not rely on stale session data
    req.currentUser = user;
    next();
  } catch (err) {
    logger.error('Authentication middleware error', {
      event: 'auth.middleware_error',
      error: err.message,
      correlationId: req.correlationId
    });
    next(err);
  }
}

/**
 * Soft authenticate: attach user if logged in, but allow unauthenticated access.
 * Used for public pages that show different content when logged in.
 */
async function softAuthenticate(req, res, next) {
  if (!req.session || !req.session.userId) {
    req.currentUser = null;
    return next();
  }

  try {
    const user = await User.findByPk(req.session.userId, {
      attributes: ['id', 'username', 'email', 'role', 'isActive']
    });

    req.currentUser = (user && user.isActive) ? user : null;
    next();
  } catch (err) {
    req.currentUser = null;
    next();
  }
}

/**
 * Redirect to login for browser requests; send 401 for API requests.
 */
function redirectOrUnauthorized(req, res) {
  if (req.accepts('html')) {
    return res.redirect('/auth/login');
  }
  return res.status(401).json({ error: 'Authentication required' });
}

module.exports = { authenticate, softAuthenticate };
