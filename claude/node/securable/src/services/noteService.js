'use strict';

const { Op } = require('sequelize');
const { Note, User, Attachment, Rating, ShareLink } = require('../models');
const auditService = require('./auditService');

/**
 * Note service — all note CRUD and search operations.
 * Ownership is verified server-side (Derived Integrity Principle — client never
 * supplies the owner userId directly; it is always derived from the session).
 */

async function createNote({ userId, title, content, isPublic = false }) {
  // Server assigns userId — never from client input
  const note = await Note.create({ userId, title: title.trim(), content, isPublic: !!isPublic });
  await auditService.record({ userId, action: 'NOTE_CREATE', targetType: 'Note', targetId: note.id });
  return note;
}

async function getNoteById(noteId) {
  return Note.findByPk(noteId, {
    include: [
      { model: User, as: 'owner', attributes: ['id', 'username'] },
      { model: Attachment, as: 'attachments' },
      {
        model: Rating,
        as: 'ratings',
        include: [{ model: User, as: 'rater', attributes: ['id', 'username'] }],
      },
    ],
  });
}

async function assertOwnership(noteId, userId) {
  const note = await Note.findOne({ where: { id: noteId, userId } });
  if (!note) {
    const err = new Error('Note not found or access denied.');
    err.status = 403;
    throw err;
  }
  return note;
}

async function updateNote(noteId, userId, { title, content, isPublic }) {
  const note = await assertOwnership(noteId, userId);
  await note.update({
    title: title.trim(),
    content,
    isPublic: !!isPublic,
  });
  await auditService.record({ userId, action: 'NOTE_UPDATE', targetType: 'Note', targetId: noteId });
  return note;
}

async function deleteNote(noteId, userId, isAdmin = false) {
  const where = isAdmin ? { id: noteId } : { id: noteId, userId };
  const note = await Note.findOne({ where });
  if (!note) {
    const err = new Error('Note not found or access denied.');
    err.status = 403;
    throw err;
  }
  await note.destroy();
  await auditService.record({ userId, action: 'NOTE_DELETE', targetType: 'Note', targetId: noteId });
}

async function getUserNotes(userId) {
  return Note.findAll({
    where: { userId },
    include: [{ model: Rating, as: 'ratings', attributes: ['value'] }],
    order: [['created_at', 'DESC']],
  });
}

async function searchNotes(query, requestingUserId) {
  const term = `%${query.trim()}%`;
  return Note.findAll({
    where: {
      [Op.and]: [
        {
          [Op.or]: [
            { title: { [Op.like]: term } },
            { content: { [Op.like]: term } },
          ],
        },
        {
          [Op.or]: [
            { userId: requestingUserId },
            { isPublic: true },
          ],
        },
      ],
    },
    include: [{ model: User, as: 'owner', attributes: ['id', 'username'] }],
    order: [['created_at', 'DESC']],
    limit: 100,
  });
}

async function getTopRatedNotes(minRatings = 3, limit = 20) {
  const { sequelize } = require('../models');
  const { fn, col, literal } = require('sequelize');

  return Note.findAll({
    attributes: {
      include: [
        [fn('AVG', col('ratings.value')), 'avgRating'],
        [fn('COUNT', col('ratings.id')), 'ratingCount'],
      ],
    },
    include: [
      { model: Rating, as: 'ratings', attributes: [] },
      { model: User, as: 'owner', attributes: ['id', 'username'] },
    ],
    where: { isPublic: true },
    group: ['Note.id', 'owner.id'],
    having: literal(`COUNT(ratings.id) >= ${minRatings}`),
    order: [[literal('avgRating'), 'DESC']],
    limit,
    subQuery: false,
  });
}

async function reassignNote(noteId, newUserId, adminUserId) {
  const note = await Note.findByPk(noteId);
  if (!note) {
    const err = new Error('Note not found.');
    err.status = 404;
    throw err;
  }
  const previousUserId = note.userId;
  await note.update({ userId: newUserId });
  await auditService.record({
    userId: adminUserId,
    action: 'NOTE_REASSIGN',
    targetType: 'Note',
    targetId: noteId,
    metadata: { from: previousUserId, to: newUserId },
  });
  return note;
}

module.exports = {
  createNote,
  getNoteById,
  assertOwnership,
  updateNote,
  deleteNote,
  getUserNotes,
  searchNotes,
  getTopRatedNotes,
  reassignNote,
};
