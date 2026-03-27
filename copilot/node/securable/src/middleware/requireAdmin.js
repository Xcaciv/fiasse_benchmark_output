'use strict';

const logger = require('../utils/logger');

/**
 * Requires admin role. Returns 403 for non-admin authenticated users.
 */
function requireAdmin(req, res, next) {
  if (req.user && req.user.role === 'admin') {
    return next();
  }
  logger.warn('Unauthorized admin access attempt', {
    userId: req.user ? req.user.id : null,
    path: req.path,
    ip: req.ip,
  });
  res.status(403).render('error', {
    statusCode: 403,
    message: 'You do not have permission to access this page.',
  });
}

module.exports = requireAdmin;
