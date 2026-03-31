'use strict';

const logger = require('../config/logger');

/**
 * Centralised error handler — SSEM Resilience / Confidentiality.
 * Stack traces and internal messages are logged but NOT exposed to clients.
 * Clients receive a generic message to avoid information leakage.
 */
function errorHandler(err, req, res, next) { // eslint-disable-line no-unused-vars
  const status = err.status || err.statusCode || 500;

  logger.error({
    event: 'UNHANDLED_ERROR',
    status,
    message: err.message,
    stack: err.stack,
    path: req.path,
    method: req.method,
    userId: req.user?.id,
  });

  // Never expose internal details to the client
  const clientMessage =
    status < 500 ? err.message : 'An unexpected error occurred. Please try again later.';

  if (req.accepts('html')) {
    res.status(status).render('error', {
      title: 'Error',
      status,
      message: clientMessage,
    });
  } else {
    res.status(status).json({ error: clientMessage });
  }
}

function notFoundHandler(req, res) {
  res.status(404).render('error', {
    title: 'Page Not Found',
    status: 404,
    message: 'The page you requested could not be found.',
  });
}

module.exports = { errorHandler, notFoundHandler };
