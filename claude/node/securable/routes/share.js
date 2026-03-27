'use strict';

const express = require('express');
const { ShareLink, Note, User, Attachment } = require('../models');

const router = express.Router();

// Public share link view — no authentication required
// [TRUST BOUNDARY] Token is the only credential; revocation is enforced here
router.get('/:token', async (req, res, next) => {
  try {
    // Validate token format before querying — prevent unnecessary DB calls
    const { token } = req.params;
    if (!/^[0-9a-f]{64}$/.test(token)) {
      return res.status(404).render('error', { title: 'Not Found', message: 'Invalid share link.', statusCode: 404 });
    }

    const shareLink = await ShareLink.findOne({
      where: { token, isRevoked: false },
      include: [{
        model: Note,
        as: 'note',
        include: [
          { model: User, as: 'owner', attributes: ['username'] },
          { model: Attachment, as: 'attachments', attributes: ['id', 'originalFilename', 'mimeType', 'fileSizeBytes'] },
        ],
      }],
    });

    if (!shareLink) {
      return res.status(404).render('error', { title: 'Not Found', message: 'Share link not found or has been revoked.', statusCode: 404 });
    }

    return res.render('share/view', {
      title: shareLink.note.title,
      note: shareLink.note,
      sharedView: true,
    });
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
