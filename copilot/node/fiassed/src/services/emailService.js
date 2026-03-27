'use strict';
const logger = require('../utils/logger');

function sendPasswordResetEmail(email, token) {
  logger.info('PASSWORD_RESET_EMAIL_STUB', { email, tokenLength: token.length });
  console.log(`[EMAIL STUB] Password reset link for ${email}: /reset-password/${token}`);
}

module.exports = { sendPasswordResetEmail };
