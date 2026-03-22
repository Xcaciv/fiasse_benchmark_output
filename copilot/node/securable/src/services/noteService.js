'use strict';
const { Op } = require('sequelize');
const { NOTE_EXCERPT_LENGTH, MIN_RATINGS_FOR_TOP_RATED } = require('../config/constants');
const { canonicalize } = require('../utils/inputHandler');

async function createNote({ title, content, userId }, { Note }) {
  return Note.create({ title: canonicalize(title), content: canonicalize(content), userId });
}

async function updateNote(noteId, userId, { title, content, isPublic }, { Note }) {
  const note = await Note.findOne({ where: { id: noteId, userId } });
  if (!note) return { success: false, error: 'Note not found or access denied' };
  await note.update({
    title: canonicalize(title),
    content: canonicalize(content),
    isPublic: Boolean(isPublic)
  });
  return { success: true, note };
}

async function deleteNote(noteId, userId, isAdmin, { Note }) {
  const where = isAdmin ? { id: noteId } : { id: noteId, userId };
  const note = await Note.findOne({ where });
  if (!note) return { success: false, error: 'Note not found or access denied' };
  await note.destroy();
  return { success: true };
}

async function getNoteById(noteId, requestingUserId, isAdmin, { Note, User }) {
  const note = await Note.findByPk(noteId, { include: [{ model: User, attributes: ['id', 'username'] }] });
  if (!note) return null;
  const canAccess = isAdmin || note.isPublic || (requestingUserId && note.userId === requestingUserId);
  return canAccess ? note : null;
}

async function searchNotes(query, requestingUserId, { Note, User }) {
  const sanitizedQuery = canonicalize(query);
  const notes = await Note.findAll({
    where: {
      [Op.and]: [
        {
          [Op.or]: [
            { title: { [Op.like]: `%${sanitizedQuery}%` } },
            { content: { [Op.like]: `%${sanitizedQuery}%` } }
          ]
        },
        {
          [Op.or]: [
            { isPublic: true },
            ...(requestingUserId ? [{ userId: requestingUserId }] : [])
          ]
        }
      ]
    },
    include: [{ model: User, attributes: ['id', 'username'] }]
  });
  return notes.map(note => ({
    ...note.toJSON(),
    excerpt: note.content.substring(0, NOTE_EXCERPT_LENGTH)
  }));
}

async function getTopRatedNotes({ Note, Rating, User, sequelize }) {
  const { fn, col, literal } = sequelize;
  const notes = await Note.findAll({
    where: { isPublic: true },
    include: [
      { model: Rating, attributes: [] },
      { model: User, attributes: ['id', 'username'] }
    ],
    attributes: {
      include: [[fn('AVG', col('Ratings.stars')), 'avgRating'],
                [fn('COUNT', col('Ratings.id')), 'ratingCount']]
    },
    group: ['Note.id'],
    having: literal(`COUNT(\`Ratings\`.\`id\`) >= ${MIN_RATINGS_FOR_TOP_RATED}`),
    order: [[literal('avgRating'), 'DESC']]
  });
  return notes;
}

async function getUserNotes(userId, { Note }) {
  return Note.findAll({ where: { userId }, order: [['createdAt', 'DESC']] });
}

async function reassignNote(noteId, newOwnerId, adminId, { Note, User }) {
  const note = await Note.findByPk(noteId);
  if (!note) return { success: false, error: 'Note not found' };
  const newOwner = await User.findByPk(newOwnerId);
  if (!newOwner) return { success: false, error: 'New owner not found' };
  await note.update({ userId: newOwnerId });
  return { success: true };
}

module.exports = { createNote, updateNote, deleteNote, getNoteById, searchNotes, getTopRatedNotes, getUserNotes, reassignNote };
