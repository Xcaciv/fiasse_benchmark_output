'use strict';

const { Rating, Note } = require('../models');
const constants = require('../config/constants');
const auditService = require('./auditService');

/**
 * Create a rating for a note.
 * Prevents self-rating and duplicate ratings at the service level.
 * @param {string} noteId
 * @param {string} userId - Authenticated user
 * @param {number} value - 1..5
 * @param {string} [comment]
 * @param {string} [correlationId]
 * @param {string} [ip]
 * @returns {Promise<{ rating: Rating|null, error: string|null }>}
 */
async function createRating(noteId, userId, value, comment, correlationId, ip) {
  const note = await Note.findByPk(noteId);
  if (!note) return { rating: null, error: 'Note not found' };

  // Integrity: self-rating prevention enforced in service, not just UI
  if (note.ownerId === userId) {
    await auditService.log('rating.self_rating_attempt', {
      actorId: userId,
      targetId: noteId,
      targetType: 'note',
      outcome: 'denied',
      ip,
      correlationId
    });
    return { rating: null, error: 'You cannot rate your own note' };
  }

  const existingRating = await Rating.findOne({ where: { noteId, userId } });
  if (existingRating) {
    return { rating: null, error: 'You have already rated this note' };
  }

  const parsedValue = parseInt(value, 10);
  if (
    isNaN(parsedValue) ||
    parsedValue < constants.RATINGS.MIN_VALUE ||
    parsedValue > constants.RATINGS.MAX_VALUE
  ) {
    return { rating: null, error: 'Rating value must be between 1 and 5' };
  }

  const truncatedComment = comment
    ? comment.slice(0, constants.RATINGS.MAX_COMMENT_LENGTH)
    : null;

  const rating = await Rating.create({
    noteId,
    userId,
    value: parsedValue,
    comment: truncatedComment
  });

  await auditService.log('rating.created', {
    actorId: userId,
    targetId: rating.id,
    targetType: 'rating',
    outcome: 'success',
    metadata: { noteId, value: parsedValue },
    ip,
    correlationId
  });

  return { rating, error: null };
}

/**
 * Update an existing rating after verifying ownership.
 * @param {string} ratingId
 * @param {string} userId
 * @param {number} value
 * @param {string} [comment]
 * @param {string} [correlationId]
 * @returns {Promise<{ rating: Rating|null, error: string|null }>}
 */
async function updateRating(ratingId, userId, value, comment, correlationId) {
  const rating = await Rating.findByPk(ratingId);
  if (!rating) return { rating: null, error: 'Rating not found' };

  if (rating.userId !== userId) {
    return { rating: null, error: 'You do not own this rating' };
  }

  const parsedValue = parseInt(value, 10);
  if (
    isNaN(parsedValue) ||
    parsedValue < constants.RATINGS.MIN_VALUE ||
    parsedValue > constants.RATINGS.MAX_VALUE
  ) {
    return { rating: null, error: 'Rating value must be between 1 and 5' };
  }

  const truncatedComment = comment
    ? comment.slice(0, constants.RATINGS.MAX_COMMENT_LENGTH)
    : null;

  await rating.update({ value: parsedValue, comment: truncatedComment });

  return { rating, error: null };
}

/**
 * Retrieve all ratings for a note.
 * Only the note owner or admin should call this; caller enforces access.
 * @param {string} noteId
 * @param {string} requestingUserId
 * @returns {Promise<Rating[]|null>}
 */
async function getRatingsForNote(noteId, requestingUserId) {
  const note = await Note.findByPk(noteId);
  if (!note) return null;

  if (note.ownerId !== requestingUserId) return null;

  return Rating.findAll({
    where: { noteId },
    order: [['createdAt', 'DESC']]
  });
}

/**
 * Compute average rating and count for a note (public display).
 * @param {string} noteId
 * @returns {Promise<{ avg: number, count: number }>}
 */
async function getAggregateRating(noteId) {
  const ratings = await Rating.findAll({
    where: { noteId },
    attributes: ['value']
  });

  if (ratings.length === 0) return { avg: 0, count: 0 };

  const sum = ratings.reduce((acc, r) => acc + r.value, 0);
  return {
    avg: Math.round((sum / ratings.length) * 10) / 10,
    count: ratings.length
  };
}

module.exports = {
  createRating,
  updateRating,
  getRatingsForNote,
  getAggregateRating
};
