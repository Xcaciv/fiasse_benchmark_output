'use strict';
const Tokens = require('csrf');

const tokens = new Tokens();
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

function csrfMiddleware(req, res, next) {
  // Initialize CSRF secret in session if not present
  if (!req.session.csrfSecret) {
    req.session.csrfSecret = tokens.secretSync();
  }

  const secret = req.session.csrfSecret;

  // Generate a token for use in templates
  res.locals.csrfToken = tokens.create(secret);

  // Skip validation for safe HTTP methods
  if (SAFE_METHODS.has(req.method)) {
    return next();
  }

  // Extract token from body or header
  const token =
    (req.body && req.body._csrf) ||
    req.headers['x-csrf-token'] ||
    req.headers['x-xsrf-token'];

  if (!token || !tokens.verify(secret, token)) {
    res.status(403);
    return res.render('errors/500', { message: 'Invalid or missing CSRF token' });
  }

  next();
}

module.exports = csrfMiddleware;
