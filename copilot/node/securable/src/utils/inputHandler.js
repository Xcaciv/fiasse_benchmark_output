'use strict';
const sanitizeHtmlLib = require('sanitize-html');

// Integrity: Canonicalize input before sanitization and validation
function canonicalize(value) {
  if (value === null || value === undefined) return '';
  return String(value).trim().normalize('NFC');
}

// Integrity: Strip all HTML tags — use for plain text fields
function sanitizeHtml(value) {
  return sanitizeHtmlLib(canonicalize(value), { allowedTags: [], allowedAttributes: {} });
}

// Integrity: Allow a safe subset of HTML — use for rich content fields
function sanitizeRichHtml(value) {
  return sanitizeHtmlLib(canonicalize(value), {
    allowedTags: ['b', 'i', 'p', 'ul', 'ol', 'li', 'br', 'em', 'strong'],
    allowedAttributes: {}
  });
}

// Integrity: Email format validation
function validateEmail(value) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(String(value).trim().toLowerCase());
}

// Integrity: Password strength — min 8 chars, at least one letter and one digit
function validatePasswordStrength(value) {
  if (!value || value.length < 8) return false;
  return /[a-zA-Z]/.test(value) && /\d/.test(value);
}

module.exports = {
  canonicalize,
  sanitizeHtml,
  sanitizeRichHtml,
  validateEmail,
  validatePasswordStrength
};
