'use strict';

const nodemailer = require('nodemailer');
const logger = require('../config/logger');

// [TRUST BOUNDARY] Email service — Logging stub when SMTP not configured.
// Tokens in emails are NOT logged — Confidentiality.

function createTransport() {
  if (process.env.SMTP_HOST) {
    return nodemailer.createTransport({
      host: process.env.SMTP_HOST,
      port: parseInt(process.env.SMTP_PORT, 10) || 587,
      secure: false,
      auth: {
        user: process.env.SMTP_USER,
        pass: process.env.SMTP_PASS,
      },
    });
  }
  // Logging stub transport — dev/test environments
  return {
    sendMail: async (opts) => {
      logger.info('Email (stub)', {
        to: opts.to,
        subject: opts.subject,
        // token URL logged only in non-production — remove for prod
        ...(process.env.NODE_ENV !== 'production' && { text: opts.text }),
      });
    },
  };
}

const transport = createTransport();

async function sendPasswordResetEmail(toEmail, resetUrl) {
  const opts = {
    from: process.env.EMAIL_FROM || 'noreply@loosenotes.local',
    to: toEmail,
    subject: 'Password Reset Request',
    text: `You requested a password reset. Use the following link (valid 1 hour):\n\n${resetUrl}\n\nIf you did not request this, ignore this email.`,
  };

  try {
    await transport.sendMail(opts);
    logger.info('Password reset email sent', { to: toEmail });
  } catch (err) {
    logger.error('Failed to send password reset email', { to: toEmail, error: err.message });
    throw err;
  }
}

module.exports = { sendPasswordResetEmail };
