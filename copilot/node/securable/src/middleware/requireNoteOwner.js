'use strict';

const { Note } = require('../models');
const logger = require('../utils/logger');

/**
 * Middleware factory that verifies note ownership.
 * options.adminAllowed: if true, admins bypass ownership check.
 */
function requireNoteOwner(options = { adminAllowed: false }) {
  return async (req, res, next) => {
    try {
      const noteId = req.params.id || req.params.noteId;
      const note = await Note.findByPk(noteId);

      if (!note) {
        return res.status(404).render('error', { statusCode: 404, message: 'Note not found.' });
      }

      const isOwner = req.user.id === note.userId;
      const isAdmin = options.adminAllowed && req.user.role === 'admin';

      if (isOwner || isAdmin) {
        req.note = note;
        return next();
      }

      logger.warn('Unauthorized note access attempt', {
        userId: req.user.id,
        noteId,
        path: req.path,
        ip: req.ip,
      });

      return res.status(403).render('error', { statusCode: 403, message: 'Access denied.' });
    } catch (err) {
      next(err);
    }
  };
}

module.exports = requireNoteOwner;
