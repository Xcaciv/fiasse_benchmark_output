'use strict';

const { Op } = require('sequelize');
const { Note, User, Rating, Attachment, ShareLink, sequelize } = require('../models');
const constants = require('../config/constants');
const auditService = require('./auditService');
const fileService = require('./fileService');

/**
 * Create a new note.
 * ownerId is always taken from the authenticated session, never from request body.
 * @param {string} ownerId - Authenticated user UUID
 * @param {{ title: string, content: string, visibility: string }} data
 * @param {string} [correlationId]
 * @param {string} [ip]
 * @returns {Promise<Note>}
 */
async function createNote(ownerId, data, correlationId, ip) {
  // Integrity: visibility validated against allowlist
  const visibility = data.visibility === 'public' ? 'public' : 'private';

  const note = await Note.create({
    title: data.title,
    content: data.content,
    visibility,
    ownerId
  });

  await auditService.log('note.created', {
    actorId: ownerId,
    targetId: note.id,
    targetType: 'note',
    outcome: 'success',
    metadata: { visibility },
    ip,
    correlationId
  });

  return note;
}

/**
 * Retrieve a note by ID, enforcing visibility and ownership rules.
 * Returns null (not throws) for unauthorized access to prevent enumeration.
 * @param {string} id
 * @param {string|null} requestingUserId
 * @returns {Promise<Note|null>}
 */
async function getNoteById(id, requestingUserId) {
  const note = await Note.findByPk(id, {
    include: [
      { model: User, as: 'owner', attributes: ['id', 'username'] },
      { model: Attachment, as: 'attachments', attributes: ['id', 'originalFilename', 'storedFilename', 'mimeType', 'size', 'createdAt'] }
    ]
  });

  if (!note) return null;

  // Visibility enforced server-side on every access
  if (note.visibility === 'private') {
    if (!requestingUserId || note.ownerId !== requestingUserId) {
      return null;
    }
  }

  return note;
}

/**
 * List notes owned by a user with pagination.
 * @param {string} ownerId
 * @param {number} page
 * @param {number} limit
 * @returns {Promise<{ rows: Note[], count: number }>}
 */
async function listUserNotes(ownerId, page = 1, limit = 10) {
  const offset = (page - 1) * limit;
  const safeLimit = Math.min(limit, constants.PAGINATION.MAX_PAGE_SIZE);

  return Note.findAndCountAll({
    where: { ownerId },
    order: [['updatedAt', 'DESC']],
    limit: safeLimit,
    offset
  });
}

/**
 * Update a note after re-verifying ownership on every call.
 * @param {string} id
 * @param {string} requestingUserId
 * @param {{ title?: string, content?: string, visibility?: string }} data
 * @param {string} [correlationId]
 * @param {string} [ip]
 * @returns {Promise<Note|null>}
 */
async function updateNote(id, requestingUserId, data, correlationId, ip) {
  // Integrity: ownership re-verified from DB on every update
  const note = await Note.findByPk(id);
  if (!note || note.ownerId !== requestingUserId) return null;

  const updates = {};
  if (data.title !== undefined) updates.title = data.title;
  if (data.content !== undefined) updates.content = data.content;
  if (data.visibility !== undefined) {
    updates.visibility = data.visibility === 'public' ? 'public' : 'private';
  }

  await note.update(updates);

  await auditService.log('note.updated', {
    actorId: requestingUserId,
    targetId: id,
    targetType: 'note',
    outcome: 'success',
    ip,
    correlationId
  });

  return note;
}

/**
 * Delete a note with cascading cleanup: shareLinks -> attachments -> ratings -> note.
 * Admins may delete any note; regular users only their own.
 * @param {string} id
 * @param {string} requestingUserId
 * @param {string} requestingUserRole
 * @param {string} [correlationId]
 * @param {string} [ip]
 * @returns {Promise<boolean>}
 */
async function deleteNote(id, requestingUserId, requestingUserRole, correlationId, ip) {
  const note = await Note.findByPk(id, {
    include: [{ model: Attachment, as: 'attachments' }]
  });

  if (!note) return false;

  const isOwner = note.ownerId === requestingUserId;
  const isAdmin = requestingUserRole === constants.ROLES.ADMIN;

  if (!isOwner && !isAdmin) return false;

  // Cascade delete in transaction for atomicity
  await sequelize.transaction(async (t) => {
    await ShareLink.destroy({ where: { noteId: id }, transaction: t });

    // Delete attachment files from disk before DB records
    for (const attachment of note.attachments || []) {
      await fileService.deleteFileFromDisk(attachment.storedFilename);
    }
    await Attachment.destroy({ where: { noteId: id }, transaction: t });

    await Rating.destroy({ where: { noteId: id }, transaction: t });
    await note.destroy({ transaction: t });
  });

  await auditService.log('note.deleted', {
    actorId: requestingUserId,
    targetId: id,
    targetType: 'note',
    outcome: 'success',
    metadata: { deletedBy: isAdmin && !isOwner ? 'admin' : 'owner' },
    ip,
    correlationId
  });

  return true;
}

/**
 * Search notes the requesting user can access.
 * Only their own notes + public notes, parameterized query.
 * @param {string} query
 * @param {string} requestingUserId
 * @param {number} page
 * @returns {Promise<{ rows: Note[], count: number }>}
 */
async function searchNotes(query, requestingUserId, page = 1) {
  const limit = constants.PAGINATION.DEFAULT_PAGE_SIZE;
  const offset = (page - 1) * limit;

  const sanitizedQuery = `%${query}%`;

  return Note.findAndCountAll({
    where: {
      [Op.and]: [
        {
          [Op.or]: [
            { title: { [Op.like]: sanitizedQuery } },
            { content: { [Op.like]: sanitizedQuery } }
          ]
        },
        {
          [Op.or]: [
            { visibility: 'public' },
            { ownerId: requestingUserId }
          ]
        }
      ]
    },
    include: [{ model: User, as: 'owner', attributes: ['username'] }],
    order: [['updatedAt', 'DESC']],
    limit,
    offset
  });
}

/**
 * Get top-rated public notes with at least MIN_RATERS_FOR_TOP distinct raters.
 * @param {number} page
 * @param {number} limit
 * @returns {Promise<Note[]>}
 */
async function getTopRated(page = 1, limit = 10) {
  const offset = (page - 1) * limit;

  // Subquery: find noteIds with at least 3 distinct raters
  const qualifiedNoteIds = await Rating.findAll({
    attributes: ['noteId'],
    group: ['noteId'],
    having: sequelize.where(
      sequelize.fn('COUNT', sequelize.fn('DISTINCT', sequelize.col('userId'))),
      { [Op.gte]: constants.RATINGS.MIN_RATERS_FOR_TOP }
    ),
    raw: true
  });

  const noteIds = qualifiedNoteIds.map((r) => r.noteId);
  if (noteIds.length === 0) return [];

  const notes = await Note.findAll({
    where: {
      id: { [Op.in]: noteIds },
      visibility: 'public'
    },
    include: [
      { model: User, as: 'owner', attributes: ['username'] },
      {
        model: Rating,
        as: 'ratings',
        attributes: []
      }
    ],
    attributes: {
      include: [
        [
          sequelize.fn('AVG', sequelize.col('ratings.value')),
          'avgRating'
        ],
        [
          sequelize.fn('COUNT', sequelize.col('ratings.id')),
          'ratingCount'
        ]
      ]
    },
    group: ['Note.id', 'owner.id'],
    order: [[sequelize.literal('avgRating'), 'DESC']],
    limit,
    offset,
    subQuery: false
  });

  return notes;
}

/**
 * Transfer note ownership atomically with audit log.
 * Admin-only operation.
 * @param {string} noteId
 * @param {string} newOwnerId
 * @param {string} adminId
 * @param {string} [correlationId]
 * @param {string} [ip]
 * @returns {Promise<boolean>}
 */
async function transferOwnership(noteId, newOwnerId, adminId, correlationId, ip) {
  const note = await Note.findByPk(noteId);
  if (!note) return false;

  const newOwner = await User.findByPk(newOwnerId);
  if (!newOwner) return false;

  const previousOwnerId = note.ownerId;

  await sequelize.transaction(async (t) => {
    await note.update({ ownerId: newOwnerId }, { transaction: t });
    await auditService.log('note.ownership_transferred', {
      actorId: adminId,
      targetId: noteId,
      targetType: 'note',
      outcome: 'success',
      metadata: { previousOwnerId, newOwnerId },
      ip,
      correlationId
    });
  });

  return true;
}

module.exports = {
  createNote,
  getNoteById,
  listUserNotes,
  updateNote,
  deleteNote,
  searchNotes,
  getTopRated,
  transferOwnership
};
