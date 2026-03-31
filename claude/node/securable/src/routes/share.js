'use strict';

const express = require('express');
const { query } = require('express-validator');
const shareLinkService = require('../services/shareLinkService');
const noteService = require('../services/noteService');
const { handleValidationErrors } = require('../middleware/validate');

const router = express.Router();

// GET /share?token=:token — public share view, no auth required
router.get(
  '/',
  [
    query('token')
      .trim()
      .matches(/^[a-f0-9]{96}$/)
      .withMessage('Invalid share token.'),
  ],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      const { token } = req.query;
      const shareLink = await shareLinkService.resolveShareLink(token);

      if (!shareLink || !shareLink.isActive) {
        return res.status(404).render('error', {
          title: 'Link Not Found',
          status: 404,
          message: 'This share link is invalid or has been revoked.',
        });
      }

      const note = await noteService.getNoteById(shareLink.noteId);
      if (!note) {
        return res.status(404).render('error', {
          title: 'Note Not Found',
          status: 404,
          message: 'The note associated with this link no longer exists.',
        });
      }

      res.render('share/view', { title: note.title, note, shareLink });
    } catch (err) {
      next(err);
    }
  }
);

module.exports = router;
