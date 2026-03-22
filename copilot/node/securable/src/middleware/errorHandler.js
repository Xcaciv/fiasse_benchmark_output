'use strict';
const multer = require('multer');
const logger = require('../utils/logger');

// Resilience: Centralized error handler — never expose stack traces to clients
function errorHandler(err, req, res, next) {
  // Handle CSRF token errors
  if (err.code === 'EBADCSRFTOKEN') {
    return res.status(403).render('error', { message: 'Invalid form submission.', layout: 'layouts/main' });
  }

  // Handle multer errors with user-friendly messages
  if (err instanceof multer.MulterError) {
    req.flash('error', `Upload error: ${err.message}`);
    return res.redirect('back');
  }

  // Confidentiality: Log details internally, expose only generic message
  logger.error('UNHANDLED_ERROR', {
    message: err.message,
    stack: err.stack,
    url: req.originalUrl,
    method: req.method
  });

  const statusCode = err.status || 500;
  if (res.headersSent) return next(err);
  return res.status(statusCode).render('error', {
    message: statusCode === 500 ? 'An unexpected error occurred.' : err.message,
    layout: 'layouts/main'
  });
}

module.exports = { errorHandler };
