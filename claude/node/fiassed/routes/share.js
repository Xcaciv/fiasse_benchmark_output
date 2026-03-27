'use strict';

const express = require('express');
const { authenticate } = require('../middleware/authenticate');
const { shareLimiter } = require('../middleware/rateLimiter');
const shareLinkService = require('../services/shareLinkService');
const auditService = require('../services/auditService');

const router = express.Router();

// ─── Create Share Link ────────────────────────────────────────────────────────

router.post('/:noteId', authenticate, async (req, res, next) => {
  try {
    const shareLink = await shareLinkService.createShareLink(
      req.params.noteId,
      req.currentUser.id,
      null,
      req.correlationId,
      req.ip
    );

    if (!shareLink) {
      req.flash('error', 'Note not found or access denied.');
      return res.redirect('/notes');
    }

    req.flash('success', 'Share link created.');
    res.redirect(`/notes/${req.params.noteId}`);
  } catch (err) {
    next(err);
  }
});

// ─── Revoke Share Link ────────────────────────────────────────────────────────

router.delete('/:linkId', authenticate, async (req, res, next) => {
  try {
    const revoked = await shareLinkService.revokeShareLink(
      req.params.linkId,
      req.currentUser.id,
      req.correlationId,
      req.ip
    );

    if (!revoked) {
      req.flash('error', 'Share link not found or access denied.');
    } else {
      req.flash('success', 'Share link revoked.');
    }

    const referer = req.get('Referer') || '/notes';
    res.redirect(referer);
  } catch (err) {
    next(err);
  }
});

// ─── View Shared Note (public, rate-limited) ──────────────────────────────────

router.get('/view/:token', shareLimiter, async (req, res, next) => {
  try {
    const result = await shareLinkService.validateShareLink(req.params.token);

    if (!result) {
      // Return 404 for invalid/expired/revoked tokens (no info leakage)
      return res.status(404).render('error', {
        title: 'Not Found',
        message: 'Share link not found or has expired.',
        correlationId: req.correlationId,
        currentUser: null
      });
    }

    await auditService.log('share_link.viewed', {
      actorId: null,
      targetId: result.link.id,
      targetType: 'share_link',
      outcome: 'success',
      metadata: { noteId: result.note.id },
      ip: req.ip,
      correlationId: req.correlationId
    });

    // Confidentiality: share view shows note content but no PII
    res.render('share/view', {
      title: result.note.title,
      note: result.note,
      currentUser: null
    });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
