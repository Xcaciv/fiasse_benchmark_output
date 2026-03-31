const logger = require('../utils/logger');

function errorHandler(err, req, res, next) {
  logger.error('Request error', {
    error: err.message,
    stack: err.stack,
    path: req.path,
    method: req.method,
    userId: req.user?.id,
  });

  if (err.code === 'EBADCSRFTOKEN') {
    req.flash('error', 'Form submission expired. Please try again.');
    return res.redirect('back');
  }

  if (req.xhr || req.headers.accept.indexOf('json') > -1) {
    return res.status(err.status || 500).json({
      error: process.env.NODE_ENV === 'production' ? 'An error occurred' : err.message,
    });
  }

  res.status(err.status || 500).render('error', {
    user: req.user,
    message: process.env.NODE_ENV === 'production' 
      ? 'An unexpected error occurred' 
      : err.message,
    stack: process.env.NODE_ENV === 'production' ? null : err.stack,
  });
}

function notFoundHandler(req, res) {
  res.status(404).render('error', {
    user: req.user,
    message: 'Page not found',
  });
}

module.exports = errorHandler;
