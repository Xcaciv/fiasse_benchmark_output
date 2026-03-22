'use strict';
const rateLimit = require('express-rate-limit');

// Availability: Rate limiting to prevent abuse
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  message: 'Too many authentication attempts. Please try again later.',
  standardHeaders: true,
  legacyHeaders: false
});

const globalLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 100,
  message: 'Too many requests. Please try again later.',
  standardHeaders: true,
  legacyHeaders: false
});

const uploadLimiter = rateLimit({
  windowMs: 60 * 60 * 1000,
  max: 20,
  message: 'Upload limit reached. Please try again later.',
  standardHeaders: true,
  legacyHeaders: false
});

module.exports = { authLimiter, globalLimiter, uploadLimiter };
