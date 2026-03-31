'use strict';

const crypto = require('crypto');
const { ShareLink, Note } = require('../models');
const auditService = require('./auditService');

/**
 * Share link service.
 * Tokens are 48 random bytes (hex) — cryptographically unpredictable (Authenticity).
 * Revocation invalidates all existing tokens for a note.
 */

function generateToken() {
  return crypto.randomBytes(48).toString('hex');
}

async function getOrCreateShareLink(noteId, userId) {
  // Verify ownership before creating
  const note = await Note.findOne({ where: { id: noteId, userId } });
  if (!note) {
    const err = new Error('Note not found or access denied.');
    err.status = 403;
    throw err;
  }

  const existing = await ShareLink.findOne({ where: { noteId, isActive: true } });
  if (existing) {
    return existing;
  }

  const token = generateToken();
  const link = await ShareLink.create({ noteId, token });
  await auditService.record({
    userId,
    action: 'SHARE_LINK_CREATE',
    targetType: 'Note',
    targetId: noteId,
  });
  return link;
}

async function regenerateShareLink(noteId, userId) {
  const note = await Note.findOne({ where: { id: noteId, userId } });
  if (!note) {
    const err = new Error('Note not found or access denied.');
    err.status = 403;
    throw err;
  }

  // Revoke existing active links
  await ShareLink.update({ isActive: false }, { where: { noteId, isActive: true } });

  const token = generateToken();
  const link = await ShareLink.create({ noteId, token });
  await auditService.record({
    userId,
    action: 'SHARE_LINK_REGENERATE',
    targetType: 'Note',
    targetId: noteId,
  });
  return link;
}

async function revokeShareLinks(noteId, userId) {
  const note = await Note.findOne({ where: { id: noteId, userId } });
  if (!note) {
    const err = new Error('Note not found or access denied.');
    err.status = 403;
    throw err;
  }
  await ShareLink.update({ isActive: false }, { where: { noteId } });
  await auditService.record({
    userId,
    action: 'SHARE_LINK_REVOKE',
    targetType: 'Note',
    targetId: noteId,
  });
}

async function resolveShareLink(token) {
  // Only alphanumeric hex tokens expected — reject anything else
  if (!/^[a-f0-9]{96}$/.test(token)) {
    return null;
  }
  return ShareLink.findOne({
    where: { token, isActive: true },
    include: [{ association: 'note' }],
  });
}

module.exports = { getOrCreateShareLink, regenerateShareLink, revokeShareLinks, resolveShareLink };
