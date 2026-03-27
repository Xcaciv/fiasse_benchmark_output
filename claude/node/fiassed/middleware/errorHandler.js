'use strict';

const { logger } = require('../config/logger');

/**
 * Global error handler middleware.
 * Resilience: catches all unhandled errors.
 * Confidentiality: never exposes stack traces or internal details to client.
 * Returns a correlation ID so users can report issues.
 */
function errorHandler(err, req, res, next) {
  const correlationId = req.correlationId || 'unknown';
  const status = err.status || err.statusCode || 500;

  // Log full error details server-side only
  logger.error('Unhandled error', {
    event: 'app.error',
    correlationId,
    status,
    error: err.message,
    stack: err.stack,
    path: req.path,
    method: req.method,
    actorId: req.currentUser ? req.currentUser.id : 'anonymous',
    ip: req.ip
  });

  // Map status codes to user-friendly messages
  const clientMessages = {
    400: 'The request was invalid or malformed.',
    401: 'Authentication is required to access this resource.',
    403: 'You do not have permission to access this resource.',
    404: 'The requested resource was not found.',
    413: 'The uploaded file is too large.',
    429: 'Too many requests. Please wait before trying again.',
    500: 'An internal server error occurred.'
  };

  const clientMessage = clientMessages[status] || 'An unexpected error occurred.';

  // Handle CSRF token errors specifically
  if (err.code === 'EBADCSRFTOKEN') {
    return res.status(403).render('error', {
      title: 'Invalid Form Submission',
      message: 'Form submission rejected: invalid or expired security token. Please refresh and try again.',
      correlationId,
      currentUser: req.currentUser || null
    });
  }

  if (req.accepts('html')) {
    return res.status(status).render('error', {
      title: `Error ${status}`,
      message: clientMessage,
      correlationId,
      currentUser: req.currentUser || null
    });
  }

  return res.status(status).json({
    error: clientMessage,
    correlationId
  });
}

/**
 * 404 handler - must be registered before errorHandler.
 */
function notFoundHandler(req, res) {
  const correlationId = req.correlationId || 'unknown';

  logger.warn('Route not found', {
    event: 'app.not_found',
    path: req.path,
    method: req.method,
    correlationId
  });

  if (req.accepts('html')) {
    return res.status(404).render('error', {
      title: 'Page Not Found',
      message: 'The page you are looking for does not exist.',
      correlationId,
      currentUser: req.currentUser || null
    });
  }

  return res.status(404).json({ error: 'Not found', correlationId });
}

module.exports = { errorHandler, notFoundHandler };
