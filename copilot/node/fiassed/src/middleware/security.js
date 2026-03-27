'use strict';
const helmet = require('helmet');
const config = require('../config');

function applySecurityHeaders(app) {
  app.use(helmet({
    contentSecurityPolicy: {
      directives: {
        defaultSrc: ["'self'"],
        scriptSrc: ["'self'"],
        styleSrc: ["'self'", "'unsafe-inline'"],
        imgSrc: ["'self'", 'data:'],
        fontSrc: ["'self'"],
        objectSrc: ["'none'"],
        frameAncestors: ["'none'"],
        formAction: ["'self'"],
        upgradeInsecureRequests: config.nodeEnv === 'production' ? [] : null,
      },
    },
    crossOriginEmbedderPolicy: false,
    hsts: config.nodeEnv === 'production'
      ? { maxAge: 31536000, includeSubDomains: true, preload: true }
      : false,
  }));

  // Prevent MIME type sniffing
  app.use((req, res, next) => {
    res.setHeader('X-Content-Type-Options', 'nosniff');
    next();
  });
}

module.exports = { applySecurityHeaders };
