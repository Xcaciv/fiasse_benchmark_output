'use strict';

const { logger } = require('../config/logger');

/** Render a 404 page for unmatched routes. */
const notFoundHandler = (req, res) => {
  res.status(404).render('errors/404', {
    title: 'Page Not Found',
    user: res.locals.user || null,
  });
};

/**
 * Global error handler. Logs the error (without sensitive data) and
 * renders an appropriate error view. Never exposes stack traces in production.
 */
const globalErrorHandler = (err, req, res, next) => {
  const status = err.status || err.statusCode || 500;
  const isProduction = process.env.NODE_ENV === 'production';

  logger.error('Unhandled application error', {
    status,
    message: err.message,
    method: req.method,
    path: req.path,
    userId: req.session ? req.session.userId : null,
    ip: req.ip,
    // Only include stack in non-production logs
    ...(isProduction ? {} : { stack: err.stack }),
  });

  if (res.headersSent) return next(err);

  const safeMessage = isProduction
    ? 'An unexpected error occurred. Please try again later.'
    : err.message;

  if (status === 403) {
    return res.status(403).render('errors/403', {
      title: 'Access Denied',
      message: err.message || 'You do not have permission to access this resource.',
      user: res.locals.user || null,
    });
  }

  if (status === 404) {
    return res.status(404).render('errors/404', {
      title: 'Not Found',
      user: res.locals.user || null,
    });
  }

  return res.status(status).render('errors/500', {
    title: 'Server Error',
    message: safeMessage,
    user: res.locals.user || null,
  });
};

module.exports = { notFoundHandler, globalErrorHandler };
