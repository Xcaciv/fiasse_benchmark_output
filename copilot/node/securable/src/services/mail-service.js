'use strict';

const fs = require('node:fs');
const path = require('node:path');

const nodemailer = require('nodemailer');

const { dataDirectories } = require('../database');

const transporter = nodemailer.createTransport({
  streamTransport: true,
  buffer: true,
  newline: 'unix'
});

async function sendPasswordResetEmail({ to, username, resetUrl }) {
  const message = await transporter.sendMail({
    from: process.env.MAIL_FROM || 'no-reply@loose-notes.local',
    to,
    subject: 'Loose Notes password reset',
    text: `Hello ${username},\n\nUse the link below to reset your password. The link expires in one hour.\n\n${resetUrl}\n\nIf you did not request this reset, you can ignore this message.\n`,
    html: `<p>Hello ${username},</p><p>Use the link below to reset your password. The link expires in one hour.</p><p><a href="${resetUrl}">${resetUrl}</a></p><p>If you did not request this reset, you can ignore this message.</p>`
  });

  const filePath = path.join(dataDirectories.mail, `reset-${Date.now()}.eml`);
  fs.writeFileSync(filePath, message.message);
  return filePath;
}

module.exports = { sendPasswordResetEmail };
