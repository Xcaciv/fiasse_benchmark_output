'use strict';

const { Op } = require('sequelize');
const { v4: uuidv4 } = require('uuid');
const { Note, Attachment, Rating, ShareLink, User, sequelize } = require('../models/index');
const { logAction, AUDIT_ACTIONS } = require('./auditService');

const PAGE_SIZE = 20;

/** Create a new note with private visibility (Derived Integrity Principle). */
const createNote = async ({ title, content, userId, ipAddress }) => {
  const note = await Note.create({ title, content, visibility: 'private', userId });

  logAction({
    actorId: userId,
    action: AUDIT_ACTIONS.NOTE_CREATE,
    resourceType: 'Note',
    resourceId: note.id,
    metadata: { title },
    ipAddress,
  });

  return note;
};

/** Fetch a note by PK with all related data included. */
const getNoteById = async (id) => {
  return Note.findByPk(id, {
    include: [
      { model: User, attributes: ['id', 'username'] },
      { model: Attachment },
      {
        model: Rating,
        include: [{ model: User, attributes: ['id', 'username'] }],
        order: [['createdAt', 'DESC']],
      },
      { model: ShareLink },
    ],
  });
};

/** Update note fields. Enforces ownership unless caller is admin. */
const updateNote = async ({ id, userId, isAdmin, title, content, visibility, ipAddress }) => {
  const note = await Note.findByPk(id);
  if (!note) {
    const err = new Error('Note not found'); err.status = 404; throw err;
  }
  if (!isAdmin && note.userId !== userId) {
    const err = new Error('Forbidden'); err.status = 403; throw err;
  }

  await note.update({ title, content, visibility });

  logAction({
    actorId: userId,
    action: AUDIT_ACTIONS.NOTE_UPDATE,
    resourceType: 'Note',
    resourceId: id,
    metadata: { title, visibility },
    ipAddress,
  });

  return note;
};

/** Delete a note (cascade handled by DB). Ownership or admin required. */
const deleteNote = async ({ id, userId, isAdmin, ipAddress }) => {
  const note = await Note.findByPk(id);
  if (!note) {
    const err = new Error('Note not found'); err.status = 404; throw err;
  }
  if (!isAdmin && note.userId !== userId) {
    const err = new Error('Forbidden'); err.status = 403; throw err;
  }

  await note.destroy();

  logAction({
    actorId: userId,
    action: AUDIT_ACTIONS.NOTE_DELETE,
    resourceType: 'Note',
    resourceId: id,
    metadata: {},
    ipAddress,
  });
};

/**
 * Search notes: returns owned notes (any visibility) + public notes from others.
 * SQLite is case-insensitive for ASCII by default; Op.like is sufficient here.
 */
const searchNotes = async ({ query, requestingUserId, page = 1 }) => {
  const offset = (page - 1) * PAGE_SIZE;
  const likeQuery = `%${query}%`;

  const { count, rows } = await Note.findAndCountAll({
    where: {
      [Op.and]: [
        {
          [Op.or]: [
            { title: { [Op.like]: likeQuery } },
            { content: { [Op.like]: likeQuery } },
          ],
        },
        {
          [Op.or]: [
            { userId: requestingUserId },
            { visibility: 'public' },
          ],
        },
      ],
    },
    include: [{ model: User, attributes: ['id', 'username'] }],
    order: [['createdAt', 'DESC']],
    limit: PAGE_SIZE,
    offset,
    distinct: true,
  });

  return { notes: rows, total: count, page, pageSize: PAGE_SIZE };
};

/** List notes owned by a specific user, paginated. */
const getUserNotes = async ({ userId, page = 1 }) => {
  const offset = (page - 1) * PAGE_SIZE;

  const { count, rows } = await Note.findAndCountAll({
    where: { userId },
    order: [['createdAt', 'DESC']],
    limit: PAGE_SIZE,
    offset,
    distinct: true,
  });

  return { notes: rows, total: count, page, pageSize: PAGE_SIZE };
};

/** Create or replace a share link for a note. Only note owner can call. */
const generateShareLink = async ({ noteId, userId, ipAddress }) => {
  const note = await Note.findByPk(noteId);
  if (!note || note.userId !== userId) {
    const err = new Error('Forbidden'); err.status = 403; throw err;
  }

  // Remove existing link then create a fresh token (regenerate = revoke + create)
  await ShareLink.destroy({ where: { noteId } });
  const shareLink = await ShareLink.create({ noteId, token: uuidv4() });

  logAction({
    actorId: userId,
    action: AUDIT_ACTIONS.NOTE_SHARE_CREATE,
    resourceType: 'ShareLink',
    resourceId: noteId,
    metadata: {},
    ipAddress,
  });

  return shareLink;
};

/** Revoke the share link for a note. Only note owner can call. */
const revokeShareLink = async ({ noteId, userId, ipAddress }) => {
  const note = await Note.findByPk(noteId);
  if (!note || note.userId !== userId) {
    const err = new Error('Forbidden'); err.status = 403; throw err;
  }

  await ShareLink.destroy({ where: { noteId } });

  logAction({
    actorId: userId,
    action: AUDIT_ACTIONS.NOTE_SHARE_REVOKE,
    resourceType: 'ShareLink',
    resourceId: noteId,
    metadata: {},
    ipAddress,
  });
};

/** Resolve a share token to the associated note with full includes. */
const getNoteByShareToken = async (token) => {
  const shareLink = await ShareLink.findOne({
    where: { token },
    include: [
      {
        model: Note,
        include: [
          { model: User, attributes: ['id', 'username'] },
          { model: Attachment },
          {
            model: Rating,
            include: [{ model: User, attributes: ['id', 'username'] }],
          },
        ],
      },
    ],
  });

  return shareLink ? shareLink.Note : null;
};

/**
 * Return public notes with average rating ≥ 3 ratings, sorted by avg desc.
 * Uses raw aggregate functions; no user-supplied values in query.
 */
const getTopRatedNotes = async () => {
  return Note.findAll({
    where: { visibility: 'public' },
    include: [
      { model: User, attributes: ['id', 'username'] },
      { model: Rating, attributes: [] },
    ],
    attributes: {
      include: [
        [sequelize.fn('AVG', sequelize.col('Ratings.stars')), 'avgRating'],
        [sequelize.fn('COUNT', sequelize.col('Ratings.id')), 'ratingCount'],
      ],
    },
    group: ['Note.id', 'User.id'],
    having: sequelize.literal('COUNT("Ratings"."id") >= 3'),
    order: [[sequelize.literal('avgRating'), 'DESC']],
    subQuery: false,
  });
};

module.exports = {
  createNote,
  getNoteById,
  updateNote,
  deleteNote,
  searchNotes,
  getUserNotes,
  generateShareLink,
  revokeShareLink,
  getNoteByShareToken,
  getTopRatedNotes,
};
