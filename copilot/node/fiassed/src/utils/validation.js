'use strict';
const path = require('path');
const config = require('../config');

function validateEmail(email) {
  if (!email || typeof email !== 'string') {
    return { valid: false, reason: 'Email is required' };
  }
  const trimmed = email.trim();
  if (trimmed.length > 254) {
    return { valid: false, reason: 'Email is too long' };
  }
  // RFC 5322 simplified pattern
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(trimmed)) {
    return { valid: false, reason: 'Invalid email format' };
  }
  return { valid: true };
}

function validateUsername(username) {
  if (!username || typeof username !== 'string') {
    return { valid: false, reason: 'Username is required' };
  }
  const trimmed = username.trim();
  if (trimmed.length < 3) {
    return { valid: false, reason: 'Username must be at least 3 characters' };
  }
  if (trimmed.length > config.usernameMaxLength) {
    return { valid: false, reason: `Username must not exceed ${config.usernameMaxLength} characters` };
  }
  if (!/^[a-zA-Z0-9_-]+$/.test(trimmed)) {
    return { valid: false, reason: 'Username may only contain letters, numbers, underscores, and hyphens' };
  }
  return { valid: true };
}

function validatePassword(password) {
  if (!password || typeof password !== 'string') {
    return { valid: false, reason: 'Password is required' };
  }
  if (password.length < config.passwordMinLength) {
    return { valid: false, reason: `Password must be at least ${config.passwordMinLength} characters` };
  }
  if (password.length > config.passwordMaxLength) {
    return { valid: false, reason: 'Password exceeds maximum length' };
  }
  return { valid: true };
}

function validateNoteTitle(title) {
  if (!title || typeof title !== 'string') {
    return { valid: false, reason: 'Title is required' };
  }
  const trimmed = title.trim();
  if (trimmed.length === 0) {
    return { valid: false, reason: 'Title cannot be empty' };
  }
  if (trimmed.length > config.noteTitleMaxLength) {
    return { valid: false, reason: `Title must not exceed ${config.noteTitleMaxLength} characters` };
  }
  return { valid: true };
}

function validateNoteContent(content) {
  if (!content || typeof content !== 'string') {
    return { valid: false, reason: 'Content is required' };
  }
  if (content.length > config.noteContentMaxLength) {
    return { valid: false, reason: `Content must not exceed ${config.noteContentMaxLength} characters` };
  }
  return { valid: true };
}

function validateRating(rating) {
  const num = parseInt(rating, 10);
  if (isNaN(num)) {
    return { valid: false, reason: 'Rating must be a number' };
  }
  if (num < 1 || num > 5) {
    return { valid: false, reason: 'Rating must be between 1 and 5' };
  }
  return { valid: true };
}

function validateSearchQuery(query) {
  if (!query || typeof query !== 'string') {
    return { valid: false, reason: 'Search query is required' };
  }
  if (query.trim().length === 0) {
    return { valid: false, reason: 'Search query cannot be empty' };
  }
  if (query.length > config.searchQueryMaxLength) {
    return { valid: false, reason: `Search query must not exceed ${config.searchQueryMaxLength} characters` };
  }
  return { valid: true };
}

function sanitizeFilename(filename) {
  if (!filename || typeof filename !== 'string') return 'file';
  const ext = path.extname(filename).toLowerCase();
  const base = path.basename(filename, ext)
    .replace(/[^a-zA-Z0-9_-]/g, '_')
    .slice(0, 100);
  return base + ext;
}

module.exports = {
  validateEmail,
  validateUsername,
  validatePassword,
  validateNoteTitle,
  validateNoteContent,
  validateRating,
  validateSearchQuery,
  sanitizeFilename,
};
