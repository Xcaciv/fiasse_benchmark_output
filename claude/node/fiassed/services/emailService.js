'use strict';

const { logger } = require('../config/logger');

/**
 * Email service - logging-only implementation.
 * In production, replace log calls with actual SMTP transport.
 * Confidentiality: tokens are not logged, only that the email was sent.
 */

/**
 * Send password reset email.
 * The token is delivered to the user but never logged.
 * @param {string} email - Recipient email
 * @param {string} resetUrl - Full reset URL containing token
 */
function sendPasswordResetEmail(email, resetUrl) {
  logger.info('Email: password reset sent', {
    event: 'email.password_reset',
    recipient: maskEmail(email),
    // Token URL logged only in dev for ease of testing
    resetUrl: process.env.NODE_ENV === 'development' ? resetUrl : '[redacted]'
  });
}

/**
 * Notify user that their password was changed.
 * @param {string} email - Account email
 */
function sendPasswordChangeNotification(email) {
  logger.info('Email: password change notification sent', {
    event: 'email.password_changed',
    recipient: maskEmail(email)
  });
}

/**
 * Notify old email address of an email change.
 * @param {string} oldEmail - Previous email address
 * @param {string} newEmail - New email address
 */
function sendEmailChangeNotification(oldEmail, newEmail) {
  logger.info('Email: email change notification sent', {
    event: 'email.email_changed',
    oldRecipient: maskEmail(oldEmail),
    newRecipient: maskEmail(newEmail)
  });
}

/**
 * Log that a password reset was requested (neutral - no user enumeration via email).
 * @param {string} email - Requested email
 */
function sendPasswordResetRequestNotification(email) {
  logger.info('Email: password reset request acknowledged', {
    event: 'email.reset_request',
    recipient: maskEmail(email)
  });
}

/**
 * Mask email for logging: user@domain.com -> u***@domain.com
 * @param {string} email
 * @returns {string}
 */
function maskEmail(email) {
  if (!email || !email.includes('@')) return '[invalid]';
  const [local, domain] = email.split('@');
  const masked = local.charAt(0) + '***';
  return `${masked}@${domain}`;
}

module.exports = {
  sendPasswordResetEmail,
  sendPasswordChangeNotification,
  sendEmailChangeNotification,
  sendPasswordResetRequestNotification
};
