'use strict';

const express = require('express');
const noteService = require('../services/noteService');
const ratingService = require('../services/ratingService');

const router = express.Router();

// GET /share/:token — public, no authentication required
router.get('/:token', async (req, res, next) => {
  try {
    // Trust boundary: token is a UUID looked up in DB; nothing executed as code
    const note = await noteService.getNoteByShareToken(req.params.token);

    if (!note) {
      return res.status(404).render('errors/404', {
        title: 'Share Link Not Found',
        user: null,
      });
    }

    const avgRating = await ratingService.getAverageRating(note.id);

    res.render('notes/view', {
      title: note.title,
      note,
      isOwner: false,
      avgRating,
      userRating: null,
      user: null,
      sharedView: true,
    });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
