'use strict';

const { Rating, User, Note, sequelize } = require('../models/index');
const { logAction, AUDIT_ACTIONS } = require('./auditService');

/**
 * Create or update a rating for a note.
 * Users may not rate their own notes (integrity guard).
 */
const createOrUpdateRating = async ({ noteId, userId, stars, comment, ipAddress }) => {
  const note = await Note.findByPk(noteId);
  if (!note) {
    const err = new Error('Note not found'); err.status = 404; throw err;
  }

  // Derived Integrity Principle: note ownership is server-resolved, not client-supplied
  if (note.userId === userId) {
    const err = new Error('You cannot rate your own note'); err.status = 403; throw err;
  }

  const existing = await Rating.findOne({ where: { noteId, userId } });
  const action = existing ? AUDIT_ACTIONS.RATING_UPDATE : AUDIT_ACTIONS.RATING_CREATE;

  let rating;
  if (existing) {
    await existing.update({ stars: parseInt(stars, 10), comment: comment || null });
    rating = existing;
  } else {
    rating = await Rating.create({
      noteId,
      userId,
      stars: parseInt(stars, 10),
      comment: comment || null,
    });
  }

  logAction({
    actorId: userId,
    action,
    resourceType: 'Rating',
    resourceId: rating.id,
    metadata: { noteId, stars },
    ipAddress,
  });

  return rating;
};

/** Return all ratings for a note with submitter username, most recent first. */
const getNoteRatings = async (noteId) => {
  return Rating.findAll({
    where: { noteId },
    include: [{ model: User, attributes: ['id', 'username'] }],
    order: [['createdAt', 'DESC']],
  });
};

/** Compute average stars and count for a note. */
const getAverageRating = async (noteId) => {
  const result = await Rating.findOne({
    where: { noteId },
    attributes: [
      [sequelize.fn('AVG', sequelize.col('stars')), 'average'],
      [sequelize.fn('COUNT', sequelize.col('id')), 'count'],
    ],
    raw: true,
  });

  return {
    average: result && result.average ? parseFloat(result.average).toFixed(1) : null,
    count: result ? parseInt(result.count, 10) : 0,
  };
};

module.exports = { createOrUpdateRating, getNoteRatings, getAverageRating };
