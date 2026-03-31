'use strict';

const { Rating, Note } = require('../models');
const auditService = require('./auditService');

/**
 * Rating service — create/update ratings with ownership and visibility checks.
 * Users may rate public notes or notes they own.
 * Rating value is constrained 1–5 server-side (Derived Integrity).
 */

function clampRating(value) {
  const n = parseInt(value, 10);
  if (!Number.isFinite(n) || n < 1 || n > 5) {
    const err = new Error('Rating value must be between 1 and 5.');
    err.status = 400;
    throw err;
  }
  return n;
}

async function assertNoteAccessible(noteId, requestingUserId) {
  const note = await Note.findByPk(noteId, { attributes: ['id', 'userId', 'isPublic'] });
  if (!note) {
    const err = new Error('Note not found.');
    err.status = 404;
    throw err;
  }
  if (!note.isPublic && note.userId !== requestingUserId) {
    const err = new Error('You cannot rate a private note that you do not own.');
    err.status = 403;
    throw err;
  }
  return note;
}

async function upsertRating({ noteId, userId, value, comment }) {
  await assertNoteAccessible(noteId, userId);
  const safeValue = clampRating(value);
  const safeComment = comment ? String(comment).slice(0, 1000) : null;

  const [rating, created] = await Rating.upsert(
    { noteId, userId, value: safeValue, comment: safeComment },
    { conflictFields: ['note_id', 'user_id'], returning: true }
  );

  await auditService.record({
    userId,
    action: created ? 'RATING_CREATE' : 'RATING_UPDATE',
    targetType: 'Note',
    targetId: noteId,
    metadata: { value: safeValue },
  });

  return rating;
}

async function getRatingsForNote(noteId) {
  return Rating.findAll({
    where: { noteId },
    include: [{ association: 'rater', attributes: ['id', 'username'] }],
    order: [['created_at', 'DESC']],
  });
}

module.exports = { upsertRating, getRatingsForNote };
