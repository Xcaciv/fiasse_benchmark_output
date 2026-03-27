'use strict';

const helmet = require('helmet');
const { v4: uuidv4 } = require('uuid');

/**
 * Attach correlation ID to every request for traceability.
 * Analyzability: all downstream logs can be correlated to a single request.
 */
function correlationId(req, res, next) {
  req.correlationId = uuidv4();
  res.setHeader('X-Correlation-Id', req.correlationId);
  next();
}

/**
 * Helmet security headers configuration.
 * Defense-in-depth for browser security.
 */
const helmetMiddleware = helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'"],
      styleSrc: ["'self'", "'unsafe-inline'"],
      imgSrc: ["'self'", 'data:'],
      fontSrc: ["'self'"],
      objectSrc: ["'none'"],
      frameAncestors: ["'none'"],
      baseUri: ["'self'"],
      formAction: ["'self'"]
    }
  },
  crossOriginEmbedderPolicy: false,
  hsts: {
    maxAge: 31536000,
    includeSubDomains: true,
    preload: false
  },
  noSniff: true,
  frameguard: { action: 'deny' },
  referrerPolicy: { policy: 'strict-origin-when-cross-origin' },
  xssFilter: true
});

/**
 * Set Cache-Control headers for responses containing private data.
 * Confidentiality: prevent caching of sensitive content in shared proxies.
 */
function noCacheForPrivate(req, res, next) {
  res.setHeader('Cache-Control', 'no-store');
  res.setHeader('Pragma', 'no-cache');
  next();
}

module.exports = { correlationId, helmetMiddleware, noCacheForPrivate };
