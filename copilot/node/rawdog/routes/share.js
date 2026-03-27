const express = require('express');
const { ShareLink, Note, User, Attachment, Rating } = require('../models');

const router = express.Router();

// GET /share/:token
router.get('/:token', async (req, res) => {
  try {
    const shareLink = await ShareLink.findOne({
      where: { token: req.params.token },
      include: [
        {
          model: Note,
          include: [
            { model: User, attributes: ['username'] },
            { model: Attachment },
            {
              model: Rating,
              include: [{ model: User, attributes: ['username'] }],
            },
          ],
        },
      ],
    });
    if (!shareLink || !shareLink.Note) {
      return res.status(404).render('error', { title: 'Not Found', message: 'Share link not found or expired.', user: null });
    }
    const note = shareLink.Note;
    const avgRating = note.Ratings.length
      ? (note.Ratings.reduce((s, r) => s + r.value, 0) / note.Ratings.length).toFixed(1)
      : null;
    res.render('notes/shared', { title: note.title, note, avgRating });
  } catch (err) {
    console.error(err);
    res.status(500).render('error', { title: 'Error', message: 'Something went wrong.', user: null });
  }
});

module.exports = router;
