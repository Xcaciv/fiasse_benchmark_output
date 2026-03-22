'use strict';

const nodemailer = require('nodemailer');
const logger = require('./logger');

let transporter;

function getTransporter() {
  if (!transporter) {
    transporter = nodemailer.createTransport({
      host: process.env.SMTP_HOST || 'smtp.ethereal.email',
      port: parseInt(process.env.SMTP_PORT || '587', 10),
      auth: {
        user: process.env.SMTP_USER || '',
        pass: process.env.SMTP_PASS || ''
      }
    });
  }
  return transporter;
}

async function sendPasswordResetEmail(toEmail, resetToken) {
  const baseUrl = process.env.APP_BASE_URL || 'http://localhost:3000';
  const resetUrl = `${baseUrl}/auth/reset-password/${resetToken}`;
  const from = process.env.EMAIL_FROM || 'noreply@loosenotes.local';

  // Always log the reset URL in development so the app works without a real SMTP server
  logger.info('Password reset requested', { email: toEmail, resetUrl });

  if (!process.env.SMTP_USER) {
    logger.warn('SMTP not configured — reset link logged above only (not emailed)');
    return { messageId: 'dev-console-only' };
  }

  const info = await getTransporter().sendMail({
    from,
    to: toEmail,
    subject: 'Loose Notes — Password Reset',
    text: `You requested a password reset.\n\nClick the link below (valid for 1 hour):\n${resetUrl}\n\nIf you did not request this, ignore this email.`,
    html: `<p>You requested a password reset.</p>
           <p><a href="${resetUrl}">Reset your password</a> (link valid for 1 hour).</p>
           <p>If you did not request this, ignore this email.</p>`
  });

  logger.info('Password reset email sent', { messageId: info.messageId, email: toEmail });
  return info;
}

module.exports = { sendPasswordResetEmail };
