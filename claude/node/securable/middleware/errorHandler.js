'use strict';

const logger = require('../config/logger');

// Centralized error handler — Resilience: structured error handling, no stack leak to client
function errorHandler(err, req, res, next) { // eslint-disable-line no-unused-vars
  const status = err.status || err.statusCode || 500;

  // Log full error detail server-side only — Confidentiality
  logger.error('Unhandled error', {
    status,
    message: err.message,
    stack: err.stack,
    url: req.originalUrl,
    method: req.method,
    userId: req.user?.id,
  });

  // Respond with generic message in production
  const clientMessage = process.env.NODE_ENV === 'production'
    ? 'An unexpected error occurred. Please try again.'
    : err.message;

  if (req.accepts('html')) {
    return res.status(status).render('error', {
      title: 'Error',
      message: clientMessage,
      statusCode: status,
    });
  }

  return res.status(status).json({ error: clientMessage });
}

function notFoundHandler(req, res) {
  res.status(404).render('error', {
    title: 'Not Found',
    message: 'The page you requested could not be found.',
    statusCode: 404,
  });
}

module.exports = { errorHandler, notFoundHandler };
