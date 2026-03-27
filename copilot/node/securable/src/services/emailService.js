'use strict';

const nodemailer = require('nodemailer');
const logger = require('../utils/logger');

const EMAIL_TIMEOUT_MS = 10000;

function buildTransporter() {
  return nodemailer.createTransport({
    host: process.env.SMTP_HOST,
    port: parseInt(process.env.SMTP_PORT || '587', 10),
    auth: {
      user: process.env.SMTP_USER,
      pass: process.env.SMTP_PASS,
    },
  });
}

function withTimeout(promise, ms) {
  const timeout = new Promise((_, reject) =>
    setTimeout(() => reject(new Error('Email service timeout')), ms)
  );
  return Promise.race([promise, timeout]);
}

async function sendPasswordResetEmail(email, resetUrl) {
  const transporter = buildTransporter();
  const from = process.env.FROM_EMAIL || 'noreply@example.com';

  const mailOptions = {
    from,
    to: email,
    subject: 'Loose Notes — Password Reset',
    text: `You requested a password reset.\n\nClick this link to reset your password (valid for 1 hour):\n${resetUrl}\n\nIf you did not request this, ignore this email.`,
    html: `<p>You requested a password reset.</p><p><a href="${resetUrl}">Reset your password</a> (valid for 1 hour).</p><p>If you did not request this, ignore this email.</p>`,
  };

  try {
    await withTimeout(transporter.sendMail(mailOptions), EMAIL_TIMEOUT_MS);
    logger.info('Password reset email sent', { to: email });
  } catch (err) {
    logger.error('Failed to send password reset email', { error: err.message });
    throw err;
  }
}

module.exports = { sendPasswordResetEmail };
