'use strict';

const nodemailer = require('nodemailer');
const logger = require('../config/logger');

/**
 * Email service — wraps nodemailer 8.x.
 * In development (no SMTP credentials) uses a no-op transport that logs the email.
 * Token values are included in emails but NEVER logged (Confidentiality).
 *
 * nodemailer 8.x createTransport() is synchronous for known transports; the API
 * is otherwise identical to 6.x for sendMail().
 */

function buildTransport() {
  const { SMTP_HOST, SMTP_USER, SMTP_PASS, SMTP_PORT } = process.env;

  if (!SMTP_HOST || !SMTP_USER) {
    // Dev/test mode: log intent without sending, never log email body (may contain tokens)
    return {
      sendMail: async (opts) => {
        logger.info({
          event: 'EMAIL_DEV_SEND',
          to: opts.to,
          subject: opts.subject,
        });
        return { messageId: 'dev-no-op' };
      },
    };
  }

  const port = parseInt(SMTP_PORT || '587', 10);
  return nodemailer.createTransport({
    host: SMTP_HOST,
    port,
    secure: port === 465,
    auth: { user: SMTP_USER, pass: SMTP_PASS },
  });
}

const transport = buildTransport();

async function sendPasswordReset(toEmail, resetUrl) {
  const from = process.env.SMTP_FROM || 'noreply@loosenotes.local';
  await transport.sendMail({
    from,
    to: toEmail,
    subject: 'Loose Notes — Password Reset',
    text: [
      'You requested a password reset.',
      '',
      'Click the link below to set a new password (valid for 1 hour):',
      resetUrl,
      '',
      'If you did not request this, ignore this email.',
    ].join('\n'),
  });
  logger.info({ event: 'EMAIL_SENT', type: 'PASSWORD_RESET', to: toEmail });
}

module.exports = { sendPasswordReset };
