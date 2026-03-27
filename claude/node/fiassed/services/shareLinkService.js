'use strict';

const { v4: uuidv4 } = require('uuid');
const { ShareLink, Note } = require('../models');
const auditService = require('./auditService');

/**
 * Create a new share link for a note.
 * Authenticity: token is a cryptographically random UUID v4.
 * @param {string} noteId
 * @param {string} ownerId - Authenticated requesting user
 * @param {Date|null} [expiresAt] - Optional expiry
 * @param {string} [correlationId]
 * @param {string} [ip]
 * @returns {Promise<ShareLink|null>}
 */
async function createShareLink(noteId, ownerId, expiresAt, correlationId, ip) {
  const note = await Note.findByPk(noteId);
  if (!note) return null;
  if (note.ownerId !== ownerId) return null;

  const token = uuidv4();

  const shareLink = await ShareLink.create({
    noteId,
    token,
    expiresAt: expiresAt || null
  });

  await auditService.log('share_link.created', {
    actorId: ownerId,
    targetId: shareLink.id,
    targetType: 'share_link',
    outcome: 'success',
    metadata: { noteId },
    ip,
    correlationId
  });

  return shareLink;
}

/**
 * Validate a share token and return the associated note if the link is active.
 * Returns null for any invalid/expired/revoked state (no information leakage).
 * @param {string} token
 * @returns {Promise<{ link: ShareLink, note: Note }|null>}
 */
async function validateShareLink(token) {
  const link = await ShareLink.findOne({
    where: { token },
    include: [{ model: Note, as: 'note' }]
  });

  if (!link) return null;
  if (link.revokedAt) return null;
  if (link.expiresAt && new Date() > link.expiresAt) return null;
  if (!link.note) return null;

  return { link, note: link.note };
}

/**
 * Revoke a share link after verifying the requester owns the parent note.
 * @param {string} linkId
 * @param {string} ownerId
 * @param {string} [correlationId]
 * @param {string} [ip]
 * @returns {Promise<boolean>}
 */
async function revokeShareLink(linkId, ownerId, correlationId, ip) {
  const link = await ShareLink.findByPk(linkId, {
    include: [{ model: Note, as: 'note' }]
  });

  if (!link) return false;
  if (!link.note || link.note.ownerId !== ownerId) return false;

  await link.update({ revokedAt: new Date() });

  await auditService.log('share_link.revoked', {
    actorId: ownerId,
    targetId: linkId,
    targetType: 'share_link',
    outcome: 'success',
    metadata: { noteId: link.noteId },
    ip,
    correlationId
  });

  return true;
}

/**
 * Get all active share links for a note owned by the requesting user.
 * @param {string} noteId
 * @param {string} ownerId
 * @returns {Promise<ShareLink[]|null>}
 */
async function getLinksForNote(noteId, ownerId) {
  const note = await Note.findByPk(noteId);
  if (!note || note.ownerId !== ownerId) return null;

  return ShareLink.findAll({
    where: { noteId, revokedAt: null },
    order: [['createdAt', 'DESC']]
  });
}

module.exports = {
  createShareLink,
  validateShareLink,
  revokeShareLink,
  getLinksForNote
};
