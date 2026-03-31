'use strict';

const rateLimit = require('express-rate-limit');
const security = require('../config/security');

/**
 * Rate limiters — SSEM Availability.
 * Separate limiters for sensitive endpoints (login) vs general traffic.
 * Standard headers expose limit state to well-behaved clients.
 */

const loginLimiter = rateLimit({
  windowMs: security.rateLimits.login.windowMs,
  max: security.rateLimits.login.max,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many login attempts. Please try again later.' },
  skipSuccessfulRequests: true,
});

const generalLimiter = rateLimit({
  windowMs: security.rateLimits.general.windowMs,
  max: security.rateLimits.general.max,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests. Please slow down.' },
});

module.exports = { loginLimiter, generalLimiter };
