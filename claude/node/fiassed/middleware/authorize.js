'use strict';

const { User } = require('../models');
const constants = require('../config/constants');
const auditService = require('../services/auditService');
const { logger } = require('../config/logger');

/**
 * Require admin role.
 * Integrity: re-verifies role from DB on every admin request,
 * never trusts cached session role alone.
 */
async function requireAdmin(req, res, next) {
  try {
    if (!req.currentUser) {
      return res.redirect('/auth/login');
    }

    // Re-verify role from DB to prevent privilege escalation via stale session
    const freshUser = await User.findByPk(req.currentUser.id, {
      attributes: ['id', 'role', 'isActive']
    });

    if (!freshUser || !freshUser.isActive || freshUser.role !== constants.ROLES.ADMIN) {
      await auditService.log('auth.admin_access_denied', {
        actorId: req.currentUser.id,
        targetId: req.path,
        targetType: 'route',
        outcome: 'denied',
        ip: req.ip,
        correlationId: req.correlationId
      });
      return res.status(403).render('error', {
        title: 'Access Denied',
        message: 'Administrator access required.',
        correlationId: req.correlationId,
        currentUser: req.currentUser
      });
    }

    next();
  } catch (err) {
    logger.error('Authorization middleware error', {
      event: 'auth.authorize_error',
      error: err.message,
      correlationId: req.correlationId
    });
    next(err);
  }
}

/**
 * Require a specific role.
 * @param {string} role
 */
function requireRole(role) {
  return async (req, res, next) => {
    if (!req.currentUser) return res.redirect('/auth/login');

    if (req.currentUser.role !== role) {
      return res.status(403).render('error', {
        title: 'Access Denied',
        message: 'You do not have permission to access this resource.',
        correlationId: req.correlationId,
        currentUser: req.currentUser
      });
    }

    next();
  };
}

module.exports = { requireAdmin, requireRole };
