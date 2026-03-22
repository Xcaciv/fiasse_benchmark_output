const fs = require('fs/promises');
const path = require('path');
const { config } = require('../config');

function sanitizeEmailAddress(email) {
  return email.replace(/[^a-z0-9@._-]/gi, '_');
}

async function sendPasswordResetEmail(user, resetUrl) {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const filePath = path.join(
    config.outboxDir,
    `${timestamp}-${sanitizeEmailAddress(user.email)}.json`
  );

  const payload = {
    to: user.email,
    subject: 'Loose Notes password reset',
    sentAt: new Date().toISOString(),
    text: [
      `Hello ${user.username},`,
      '',
      'A password reset was requested for your Loose Notes account.',
      `Use the link below within 1 hour to set a new password:`,
      resetUrl,
      '',
      'If you did not request this, you can ignore this email.'
    ].join('\n')
  };

  await fs.writeFile(filePath, JSON.stringify(payload, null, 2), 'utf8');

  return filePath;
}

module.exports = {
  sendPasswordResetEmail
};
