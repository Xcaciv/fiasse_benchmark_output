'use strict';

/**
 * Operational (expected) application errors — distinct from programmer bugs.
 * statusCode: HTTP status to return
 * isOperational: true means safe to surface user-facing message
 */
class AppError extends Error {
  constructor(message, statusCode = 500, isOperational = true) {
    super(message);
    this.statusCode = statusCode;
    this.isOperational = isOperational;
    Error.captureStackTrace(this, this.constructor);
  }
}

class NotFoundError extends AppError {
  constructor(message = 'Resource not found') {
    super(message, 404);
  }
}

class ForbiddenError extends AppError {
  constructor(message = 'Access denied') {
    super(message, 403);
  }
}

class ValidationError extends AppError {
  constructor(message = 'Validation failed') {
    super(message, 422);
  }
}

class UnauthorizedError extends AppError {
  constructor(message = 'Authentication required') {
    super(message, 401);
  }
}

module.exports = { AppError, NotFoundError, ForbiddenError, ValidationError, UnauthorizedError };
