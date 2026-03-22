'use strict';

const crypto = require('crypto');

/**
 * Synchronised Token Pattern CSRF protection.
 * - Generates a per-session random token stored in req.session.csrfToken.
 * - Exposes it as res.locals.csrfToken for use in EJS views (<input name="_csrf">).
 * - Validates the token on every state-changing request (POST, PUT, PATCH, DELETE).
 * - Uses timing-safe comparison to prevent timing attacks.
 */
function csrfMiddleware(req, res, next) {
  // Ensure a token exists in the session
  if (!req.session.csrfToken) {
    req.session.csrfToken = crypto.randomBytes(32).toString('hex');
  }

  // Make the token available to every template
  res.locals.csrfToken = req.session.csrfToken;

  // Only validate on mutating methods
  const safeMethods = ['GET', 'HEAD', 'OPTIONS'];
  if (safeMethods.includes(req.method)) return next();

  const submitted = (req.body && req.body._csrf) || req.headers['x-csrf-token'];
  const expected  = req.session.csrfToken;

  let valid = false;
  if (submitted && submitted.length === expected.length) {
    try {
      valid = crypto.timingSafeEqual(Buffer.from(submitted), Buffer.from(expected));
    } catch (_) {
      valid = false;
    }
  }

  if (!valid) {
    return res.status(403).render('error', {
      title: 'Request Forbidden',
      message: 'CSRF token mismatch. Please go back and try again.'
    });
  }

  next();
}

module.exports = csrfMiddleware;
