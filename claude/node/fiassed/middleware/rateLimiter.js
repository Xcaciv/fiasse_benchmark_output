'use strict';

const rateLimit = require('express-rate-limit');

/**
 * Rate limiter factory.
 * Availability: limits protect against DoS and brute force.
 * @param {object} opts - express-rate-limit options
 */
function createLimiter(opts) {
  return rateLimit({
    standardHeaders: true,  // Return RateLimit-* headers
    legacyHeaders: false,
    skipSuccessfulRequests: false,
    handler: (req, res) => {
      res.status(429).render('error', {
        title: 'Too Many Requests',
        message: 'You have exceeded the request limit. Please wait before trying again.',
        correlationId: req.correlationId,
        currentUser: req.currentUser || null
      });
    },
    ...opts
  });
}

// 10 registration attempts per hour per IP
const registrationLimiter = createLimiter({
  windowMs: 60 * 60 * 1000,
  max: 10,
  message: 'Too many registration attempts'
});

// 20 login attempts per 15 minutes per IP
const loginLimiter = createLimiter({
  windowMs: 15 * 60 * 1000,
  max: 20,
  message: 'Too many login attempts'
});

// 30 searches per minute per session (keyed by IP if no session)
const searchLimiter = createLimiter({
  windowMs: 60 * 1000,
  max: 30,
  keyGenerator: (req) => (req.session && req.session.userId) ? req.session.userId : req.ip
});

// 120 top-rated page views per minute per IP
const topRatedLimiter = createLimiter({
  windowMs: 60 * 1000,
  max: 120
});

// 60 share link views per minute per IP
const shareLimiter = createLimiter({
  windowMs: 60 * 1000,
  max: 60
});

// 100 general requests per minute per IP
const generalLimiter = createLimiter({
  windowMs: 60 * 1000,
  max: 100
});

// Stricter limiter for password reset to prevent enumeration
const passwordResetLimiter = createLimiter({
  windowMs: 60 * 60 * 1000,
  max: 5,
  message: 'Too many password reset attempts'
});

module.exports = {
  registrationLimiter,
  loginLimiter,
  searchLimiter,
  topRatedLimiter,
  shareLimiter,
  generalLimiter,
  passwordResetLimiter
};
