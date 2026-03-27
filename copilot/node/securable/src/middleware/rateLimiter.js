'use strict';

const rateLimit = require('express-rate-limit');

// Skip rate limiting entirely in test environments
const skipInTest = () => process.env.NODE_ENV === 'test';

/**
 * Auth endpoints: 10 requests per 15 minutes per IP.
 * Availability: mitigates brute-force credential attacks.
 */
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  message: 'Too many authentication attempts. Please try again in 15 minutes.',
  standardHeaders: true,
  legacyHeaders: false,
  skip: skipInTest,
});

/**
 * General API endpoints: 100 requests per 15 minutes per IP.
 */
const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 100,
  message: 'Request limit exceeded. Please slow down.',
  standardHeaders: true,
  legacyHeaders: false,
  skip: skipInTest,
});

/**
 * File upload endpoints: 20 uploads per hour per IP.
 */
const uploadLimiter = rateLimit({
  windowMs: 60 * 60 * 1000,
  max: 20,
  message: 'Upload limit exceeded. Please try again later.',
  standardHeaders: true,
  legacyHeaders: false,
  skip: skipInTest,
});

module.exports = { authLimiter, apiLimiter, uploadLimiter };
