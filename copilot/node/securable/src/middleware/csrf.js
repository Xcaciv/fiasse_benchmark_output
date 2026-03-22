'use strict';

const crypto = require('node:crypto');

function ensureSessionToken(req) {
  if (!req.session.csrfToken) {
    req.session.csrfToken = crypto.randomBytes(32).toString('hex');
  }

  return req.session.csrfToken;
}

function attachCsrfToken(req, res, next) {
  req.csrfToken = () => ensureSessionToken(req);
  next();
}

function verifyCsrfToken(req, res, next) {
  if (['GET', 'HEAD', 'OPTIONS'].includes(req.method)) {
    return next();
  }

  const sessionToken = ensureSessionToken(req);
  const requestToken = req.body?._csrf || req.query?._csrf || req.get('x-csrf-token');

  if (!requestToken) {
    const error = new Error('Missing CSRF token');
    error.code = 'EBADCSRFTOKEN';
    return next(error);
  }

  const sessionBuffer = Buffer.from(sessionToken);
  const requestBuffer = Buffer.from(String(requestToken));

  if (
    sessionBuffer.length !== requestBuffer.length ||
    !crypto.timingSafeEqual(sessionBuffer, requestBuffer)
  ) {
    const error = new Error('Invalid CSRF token');
    error.code = 'EBADCSRFTOKEN';
    return next(error);
  }

  return next();
}

module.exports = {
  attachCsrfToken,
  verifyCsrfToken
};
