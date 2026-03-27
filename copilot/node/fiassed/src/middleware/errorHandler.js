'use strict';
const logger = require('../utils/logger');
const config = require('../config');

function notFoundHandler(req, res) {
  res.status(404).render('errors/404', {});
}

function errorHandler(err, req, res, next) {
  logger.error('Unhandled error', {
    error: err.message,
    stack: config.nodeEnv !== 'production' ? err.stack : undefined,
    url: req.url,
    method: req.method,
    userId: req.session ? req.session.userId : undefined,
  });

  const statusCode = err.status || err.statusCode || 500;
  res.status(statusCode);

  const message = config.nodeEnv !== 'production' ? err.message : 'An unexpected error occurred';

  if (res.headersSent) {
    return next(err);
  }

  res.render('errors/500', { message });
}

module.exports = { notFoundHandler, errorHandler };
