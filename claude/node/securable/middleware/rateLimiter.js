'use strict';

const rateLimit = require('express-rate-limit');

// Centralized rate limit definitions — Availability: protect against abuse
const windowMs = parseInt(process.env.RATE_LIMIT_WINDOW_MS, 10) || 15 * 60 * 1000; // 15 min
const maxRequests = parseInt(process.env.RATE_LIMIT_MAX_REQUESTS, 10) || 100;
const authMax = parseInt(process.env.AUTH_RATE_LIMIT_MAX, 10) || 10;

// General API rate limiter
const generalLimiter = rateLimit({
  windowMs,
  max: maxRequests,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests, please try again later.' },
  skip: (req) => req.user?.role === 'admin', // admins exempt
});

// Stricter limiter for authentication endpoints
const authLimiter = rateLimit({
  windowMs,
  max: authMax,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many authentication attempts, please try again later.' },
});

// Limiter for file upload endpoints — Availability: prevent disk exhaustion
const uploadLimiter = rateLimit({
  windowMs: 60 * 60 * 1000, // 1 hour
  max: 30,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Upload limit reached. Try again later.' },
});

module.exports = { generalLimiter, authLimiter, uploadLimiter };
