const express = require('express');
const router = express.Router();
const db = require('../models');

// GET /share/:token
router.get('/:token', async (req, res, next) => {
  try {
    const shareLink = await db.ShareLink.findOne({
      where: { token: req.params.token, isActive: true },
      include: [
        {
          model: db.Note,
          include: [
            { model: db.User, as: 'author', attributes: ['username'] },
            { model: db.Attachment },
            {
              model: db.Rating,
              include: [{ model: db.User, as: 'rater', attributes: ['username'] }]
            }
          ]
        }
      ]
    });

    if (!shareLink || !shareLink.Note) {
      return res.status(404).render('error', {
        title: 'Link Not Found',
        statusCode: 404,
        message: 'This share link is invalid or has been revoked.'
      });
    }

    const note = shareLink.Note;
    const ratings = note.Ratings || [];
    const avgRating = ratings.length > 0
      ? (ratings.reduce((sum, r) => sum + r.stars, 0) / ratings.length).toFixed(1)
      : null;

    res.render('share/view', {
      title: note.title,
      note: note.toJSON(),
      ratings,
      avgRating
    });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
