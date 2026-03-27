'use strict';

const { ShareLink, Note, User, Attachment } = require('../models');

async function getSharedNote(req, res, next) {
  try {
    const { token } = req.params;
    if (!token || !/^[a-f0-9]{64}$/i.test(token)) {
      return res.status(404).render('error', { statusCode: 404, message: 'Share link not found.' });
    }
    const shareLink = await ShareLink.findOne({
      where: { token, revokedAt: null },
      include: [{ model: Note, include: [{ model: User, as: 'author', attributes: ['username'] }, { model: Attachment }] }],
    });
    if (!shareLink || !shareLink.Note) {
      return res.status(404).render('error', { statusCode: 404, message: 'Share link not found or has been revoked.' });
    }
    const note = shareLink.Note;
    res.render('notes/view', {
      title: note.title,
      note,
      avgRating: null,
      shareLinks: [],
      isOwner: false,
      shareToken: token,
      csrfToken: req.csrfToken ? req.csrfToken() : '',
    });
  } catch (err) {
    next(err);
  }
}

module.exports = { getSharedNote };
