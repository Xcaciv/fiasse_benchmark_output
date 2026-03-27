'use strict';

const csurf = require('csurf');
const { logger } = require('../config/logger');

/**
 * Session-backed CSRF middleware.
 * Value extractor reads from body, query string, or X-CSRF-Token header.
 * Authenticity: verifies that POST/PUT/DELETE requests originate from our own forms.
 */
const csrfMiddleware = csurf({
  cookie: false, // Session-backed — no separate CSRF cookie
  value: (req) =>
    req.body._csrf ||
    req.query._csrf ||
    req.headers['x-csrf-token'],
});

/**
 * Expose the CSRF token to EJS templates via res.locals.
 * Must run after csrfMiddleware on every authenticated route.
 */
const csrfTokenHelper = (req, res, next) => {
  res.locals.csrfToken = req.csrfToken ? req.csrfToken() : '';
  next();
};

/**
 * Handle CSRF validation failures explicitly.
 * Returns 403 with a user-friendly message; never leaks internal details.
 */
const csrfErrorHandler = (err, req, res, next) => {
  if (err.code !== 'EBADCSRFTOKEN') return next(err);

  logger.warn('CSRF token validation failed', {
    method: req.method,
    path: req.path,
    ip: req.ip,
  });

  return res.status(403).render('errors/403', {
    title: 'Session Expired',
    message: 'Your form session has expired. Please go back and try again.',
    user: res.locals.user || null,
  });
};

module.exports = { csrfMiddleware, csrfTokenHelper, csrfErrorHandler };
