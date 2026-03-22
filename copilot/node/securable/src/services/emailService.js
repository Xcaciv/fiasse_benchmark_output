'use strict';
const nodemailer = require('nodemailer');

// Availability: Gracefully degrade if SMTP not configured
function createEmailService(logger) {
  const isConfigured = Boolean(
    process.env.SMTP_HOST && process.env.SMTP_USER && process.env.SMTP_PASS
  );

  let transporter = null;
  if (isConfigured) {
    transporter = nodemailer.createTransport({
      host: process.env.SMTP_HOST,
      port: parseInt(process.env.SMTP_PORT || '587', 10),
      auth: {
        user: process.env.SMTP_USER,
        pass: process.env.SMTP_PASS
      }
    });
  }

  async function sendPasswordResetEmail(email, token, baseUrl) {
    const resetUrl = `${baseUrl}/auth/reset-password/${token}`;
    if (!isConfigured) {
      // Dev mode: log reset URL (token only, no sensitive user data in log)
      logger.info('DEV_PASSWORD_RESET', { resetUrl });
      return;
    }
    await transporter.sendMail({
      from: process.env.SMTP_FROM || 'noreply@loosenotes.local',
      to: email,
      subject: 'Password Reset Request',
      text: `Click the link to reset your password: ${resetUrl}\nExpires in 1 hour.`
    });
  }

  return { sendPasswordResetEmail };
}

module.exports = { createEmailService };
