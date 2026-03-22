const express = require('express');
const router = express.Router();
const { ShareLink, Note, User, Attachment } = require('../models');

// GET /share/:token
router.get('/:token', async (req, res, next) => {
  try {
    const shareLink = await ShareLink.findOne({
      where: { token: req.params.token },
      include: [
        {
          model: Note,
          include: [
            { model: User, as: 'author', attributes: ['username'] },
            { model: Attachment },
          ],
        },
      ],
    });
    if (!shareLink || !shareLink.Note) {
      return res.status(404).render('error', { title: 'Not Found', message: 'This share link is invalid or has been revoked.', user: req.user || null });
    }
    res.render('notes/view', {
      title: shareLink.Note.title,
      user: req.user || null,
      note: shareLink.Note,
      shareLinks: [],
      avgRating: null,
      userRating: null,
      isSharedView: true,
      messages: req.flash(),
    });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
