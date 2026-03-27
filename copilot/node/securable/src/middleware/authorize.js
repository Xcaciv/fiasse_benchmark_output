'use strict';

const { Note } = require('../models/index');

/**
 * Authorize note access for the note owner only.
 * Attaches note to req.note for downstream handlers.
 */
const requireNoteOwnership = async (req, res, next) => {
  const noteId = req.params.id || req.params.noteId;

  try {
    const note = await Note.findByPk(noteId);

    if (!note) {
      return res.status(404).render('errors/404', {
        title: 'Note Not Found',
        user: res.locals.user,
      });
    }

    if (note.userId !== req.session.userId) {
      return res.status(403).render('errors/403', {
        title: 'Access Denied',
        message: 'You do not own this note.',
        user: res.locals.user,
      });
    }

    req.note = note;
    next();
  } catch (err) {
    next(err);
  }
};

/**
 * Authorize note access for the note owner OR an admin.
 * Attaches note to req.note for downstream handlers.
 */
const requireNoteOwnerOrAdmin = async (req, res, next) => {
  const noteId = req.params.id || req.params.noteId;

  try {
    const note = await Note.findByPk(noteId);

    if (!note) {
      return res.status(404).render('errors/404', {
        title: 'Note Not Found',
        user: res.locals.user,
      });
    }

    const isOwner = note.userId === req.session.userId;
    const isAdmin = req.session.role === 'admin';

    if (!isOwner && !isAdmin) {
      return res.status(403).render('errors/403', {
        title: 'Access Denied',
        message: 'You do not have permission to perform this action.',
        user: res.locals.user,
      });
    }

    req.note = note;
    next();
  } catch (err) {
    next(err);
  }
};

module.exports = { requireNoteOwnership, requireNoteOwnerOrAdmin };
