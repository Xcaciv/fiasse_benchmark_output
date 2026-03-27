'use strict';
const rateLimit = require('express-rate-limit');
const config = require('../config');

const loginLimiter = rateLimit({
  windowMs: config.loginRateLimitWindowMs,
  max: config.loginRateLimitMax,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many login attempts. Please try again later.' },
  skipSuccessfulRequests: false,
});

const registrationLimiter = rateLimit({
  windowMs: config.generalRateLimitWindowMs,
  max: 10,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many registration attempts. Please try again later.' },
});

const searchLimiter = rateLimit({
  windowMs: config.searchRateLimitWindowMs,
  max: config.searchRateLimitMax,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many search requests. Please slow down.' },
});

const generalLimiter = rateLimit({
  windowMs: config.generalRateLimitWindowMs,
  max: config.generalRateLimitMax,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests. Please try again later.' },
});

module.exports = { loginLimiter, registrationLimiter, searchLimiter, generalLimiter };
