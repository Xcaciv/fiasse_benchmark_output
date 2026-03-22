const crypto = require('crypto');
const express = require('express');
const { asyncHandler } = require('../lib/async-handler');
const { redirectWithSession } = require('../lib/auth');
const { attachmentUploadMiddleware, deleteStoredFiles } = require('../lib/files');
const { logActivity } = require('../lib/activity');
const { getDb, withTransaction } = require('../db');
const { requireAuth } = require('../middleware/auth');
const { config } = require('../config');

const router = express.Router();

function buildShareUrl(token) {
  return `${config.baseUrl}/shared/${token}`;
}

async function getNoteById(noteId) {
  const db = await getDb();
  return db.get(
    `
      SELECT
        n.*,
        u.username AS author_username,
        u.email AS author_email
      FROM notes n
      JOIN users u ON u.id = n.user_id
      WHERE n.id = ?
    `,
    [noteId]
  );
}

async function userCanAccessNote(note, user) {
  if (!note) {
    return false;
  }

  if (note.is_public) {
    return true;
  }

  if (!user) {
    return false;
  }

  return user.id === note.user_id || user.role === 'Admin';
}

async function getActiveShareLink(token) {
  const db = await getDb();
  return db.get(
    `
      SELECT *
      FROM share_links
      WHERE token = ?
        AND is_active = 1
    `,
    [token]
  );
}

async function loadNotePageData(noteId, currentUser) {
  const db = await getDb();
  const note = await getNoteById(noteId);

  if (!note) {
    return null;
  }

  const attachments = await db.all(
    `
      SELECT *
      FROM attachments
      WHERE note_id = ?
      ORDER BY created_at ASC
    `,
    [noteId]
  );

  const ratings = await db.all(
    `
      SELECT
        r.*,
        u.username AS rater_username
      FROM ratings r
      JOIN users u ON u.id = r.user_id
      WHERE r.note_id = ?
      ORDER BY r.created_at DESC
    `,
    [noteId]
  );

  const ratingSummary = await db.get(
    `
      SELECT
        ROUND(AVG(rating_value), 2) AS average_rating,
        COUNT(*) AS rating_count
      FROM ratings
      WHERE note_id = ?
    `,
    [noteId]
  );

  const currentUserRating = currentUser
    ? await db.get('SELECT * FROM ratings WHERE note_id = ? AND user_id = ?', [noteId, currentUser.id])
    : null;

  const activeShareLinks =
    currentUser && (currentUser.id === note.user_id || currentUser.role === 'Admin')
      ? await db.all(
          `
            SELECT *
            FROM share_links
            WHERE note_id = ?
            ORDER BY created_at DESC
          `,
          [noteId]
        )
      : [];

  return {
    note,
    attachments,
    ratings,
    ratingSummary,
    currentUserRating,
    activeShareLinks
  };
}

function noteFormModel(overrides = {}) {
  return {
    title: '',
    content: '',
    isPublic: false,
    ...overrides
  };
}

router.get('/notes/new', requireAuth, (req, res) => {
  res.render('notes/new', {
    pageTitle: 'Create Note',
    errors: [],
    formData: noteFormModel()
  });
});

router.post(
  '/notes',
  requireAuth,
  attachmentUploadMiddleware,
  asyncHandler(async (req, res) => {
    const db = await getDb();
    const title = (req.body.title || '').trim();
    const content = (req.body.content || '').trim();
    const isPublic = req.body.isPublic === 'on';
    const errors = [];

    if (!title) {
      errors.push('Title is required.');
    }

    if (!content) {
      errors.push('Content is required.');
    }

    if (req.uploadError) {
      errors.push(req.uploadError);
    }

    if (errors.length > 0) {
      if (req.files?.length) {
        await deleteStoredFiles(req.files.map((file) => ({ stored_filename: file.filename })));
      }

      res.status(422).render('notes/new', {
        pageTitle: 'Create Note',
        errors,
        formData: noteFormModel({ title, content, isPublic })
      });
      return;
    }

    const noteResult = await db.run(
      `
        INSERT INTO notes (user_id, title, content, is_public)
        VALUES (?, ?, ?, ?)
      `,
      [req.currentUser.id, title, content, isPublic ? 1 : 0]
    );

    for (const file of req.files || []) {
      await db.run(
        `
          INSERT INTO attachments (note_id, stored_filename, original_filename, mime_type, size_bytes)
          VALUES (?, ?, ?, ?, ?)
        `,
        [noteResult.lastID, file.filename, file.originalname, file.mimetype, file.size]
      );
    }

    await logActivity(req.currentUser.id, 'note_created', `Created note "${title}".`);
    req.flash('success', 'Note created successfully.');
    await redirectWithSession(req, res, `/notes/${noteResult.lastID}`);
  })
);

router.get(
  '/notes/:id',
  asyncHandler(async (req, res) => {
    const noteId = Number(req.params.id);
    const data = await loadNotePageData(noteId, req.currentUser);

    if (!data) {
      res.status(404).render('error', {
        pageTitle: 'Note not found',
        errorMessage: 'The requested note could not be found.'
      });
      return;
    }

    const canAccess = await userCanAccessNote(data.note, req.currentUser);

    if (!canAccess) {
      res.status(403).render('error', {
        pageTitle: 'Access denied',
        errorMessage: 'You do not have permission to view this private note.'
      });
      return;
    }

    res.render('notes/show', {
      pageTitle: data.note.title,
      ...data,
      shareToken: null,
      shareUrlForToken: buildShareUrl
    });
  })
);

router.get(
  '/shared/:token',
  asyncHandler(async (req, res) => {
    const shareLink = await getActiveShareLink(req.params.token);

    if (!shareLink) {
      res.status(404).render('error', {
        pageTitle: 'Share link unavailable',
        errorMessage: 'This share link is invalid or has been revoked.'
      });
      return;
    }

    const data = await loadNotePageData(shareLink.note_id, req.currentUser);

    if (!data) {
      res.status(404).render('error', {
        pageTitle: 'Note not found',
        errorMessage: 'The shared note could not be found.'
      });
      return;
    }

    res.render('notes/show', {
      pageTitle: `${data.note.title} (Shared)`,
      ...data,
      shareToken: shareLink.token,
      shareUrlForToken: buildShareUrl
    });
  })
);

router.get(
  '/notes/:id/edit',
  requireAuth,
  asyncHandler(async (req, res) => {
    const note = await getNoteById(Number(req.params.id));

    if (!note) {
      res.status(404).render('error', {
        pageTitle: 'Note not found',
        errorMessage: 'The requested note could not be found.'
      });
      return;
    }

    if (note.user_id !== req.currentUser.id && req.currentUser.role !== 'Admin') {
      res.status(403).render('error', {
        pageTitle: 'Access denied',
        errorMessage: 'You do not have permission to edit this note.'
      });
      return;
    }

    const db = await getDb();
    const attachments = await db.all('SELECT * FROM attachments WHERE note_id = ? ORDER BY created_at ASC', [
      note.id
    ]);

    res.render('notes/edit', {
      pageTitle: `Edit ${note.title}`,
      errors: [],
      formData: noteFormModel({
        title: note.title,
        content: note.content,
        isPublic: Boolean(note.is_public)
      }),
      note,
      attachments
    });
  })
);

router.post(
  '/notes/:id/edit',
  requireAuth,
  attachmentUploadMiddleware,
  asyncHandler(async (req, res) => {
    const db = await getDb();
    const note = await getNoteById(Number(req.params.id));

    if (!note) {
      res.status(404).render('error', {
        pageTitle: 'Note not found',
        errorMessage: 'The requested note could not be found.'
      });
      return;
    }

    if (note.user_id !== req.currentUser.id && req.currentUser.role !== 'Admin') {
      res.status(403).render('error', {
        pageTitle: 'Access denied',
        errorMessage: 'You do not have permission to edit this note.'
      });
      return;
    }

    const title = (req.body.title || '').trim();
    const content = (req.body.content || '').trim();
    const isPublic = req.body.isPublic === 'on';
    const removeAttachmentIds = []
      .concat(req.body.removeAttachmentIds || [])
      .map((value) => Number(value))
      .filter(Boolean);
    const errors = [];

    if (!title) {
      errors.push('Title is required.');
    }

    if (!content) {
      errors.push('Content is required.');
    }

    if (req.uploadError) {
      errors.push(req.uploadError);
    }

    const existingAttachments = await db.all(
      'SELECT * FROM attachments WHERE note_id = ? ORDER BY created_at ASC',
      [note.id]
    );

    if (errors.length > 0) {
      if (req.files?.length) {
        await deleteStoredFiles(req.files.map((file) => ({ stored_filename: file.filename })));
      }

      res.status(422).render('notes/edit', {
        pageTitle: `Edit ${note.title}`,
        errors,
        formData: noteFormModel({ title, content, isPublic }),
        note,
        attachments: existingAttachments
      });
      return;
    }

    const attachmentsToDelete = existingAttachments.filter((attachment) =>
      removeAttachmentIds.includes(attachment.id)
    );

    await withTransaction(async (database) => {
      await database.run(
        `
          UPDATE notes
          SET title = ?, content = ?, is_public = ?, updated_at = CURRENT_TIMESTAMP
          WHERE id = ?
        `,
        [title, content, isPublic ? 1 : 0, note.id]
      );

      if (attachmentsToDelete.length > 0) {
        const placeholders = attachmentsToDelete.map(() => '?').join(', ');
        await database.run(
          `DELETE FROM attachments WHERE id IN (${placeholders})`,
          attachmentsToDelete.map((attachment) => attachment.id)
        );
      }

      for (const file of req.files || []) {
        await database.run(
          `
            INSERT INTO attachments (note_id, stored_filename, original_filename, mime_type, size_bytes)
            VALUES (?, ?, ?, ?, ?)
          `,
          [note.id, file.filename, file.originalname, file.mimetype, file.size]
        );
      }
    });

    await deleteStoredFiles(attachmentsToDelete);
    await logActivity(req.currentUser.id, 'note_updated', `Updated note "${title}".`);
    req.flash('success', 'Note updated successfully.');
    await redirectWithSession(req, res, `/notes/${note.id}`);
  })
);

router.post(
  '/notes/:id/delete',
  requireAuth,
  asyncHandler(async (req, res) => {
    const db = await getDb();
    const note = await getNoteById(Number(req.params.id));

    if (!note) {
      req.flash('error', 'The note could not be found.');
      await redirectWithSession(req, res, '/');
      return;
    }

    if (note.user_id !== req.currentUser.id && req.currentUser.role !== 'Admin') {
      res.status(403).render('error', {
        pageTitle: 'Access denied',
        errorMessage: 'You do not have permission to delete this note.'
      });
      return;
    }

    const attachments = await db.all('SELECT * FROM attachments WHERE note_id = ?', [note.id]);

    await withTransaction(async (database) => {
      await database.run('DELETE FROM notes WHERE id = ?', [note.id]);
    });

    await deleteStoredFiles(attachments);
    await logActivity(req.currentUser.id, 'note_deleted', `Deleted note "${note.title}".`);
    req.flash('success', 'Note deleted successfully.');
    await redirectWithSession(req, res, '/');
  })
);

router.post(
  '/notes/:id/share-links/create',
  requireAuth,
  asyncHandler(async (req, res) => {
    const db = await getDb();
    const note = await getNoteById(Number(req.params.id));

    if (!note || note.user_id !== req.currentUser.id) {
      res.status(403).render('error', {
        pageTitle: 'Access denied',
        errorMessage: 'Only the note owner can manage share links.'
      });
      return;
    }

    const shouldRegenerate = req.body.mode === 'regenerate';
    const token = crypto.randomBytes(24).toString('hex');

    await withTransaction(async (database) => {
      if (shouldRegenerate) {
        await database.run(
          `
            UPDATE share_links
            SET is_active = 0, revoked_at = CURRENT_TIMESTAMP
            WHERE note_id = ? AND is_active = 1
          `,
          [note.id]
        );
      }

      await database.run(
        `
          INSERT INTO share_links (note_id, token, created_by_user_id)
          VALUES (?, ?, ?)
        `,
        [note.id, token, req.currentUser.id]
      );
    });

    await logActivity(
      req.currentUser.id,
      'share_link_created',
      `Created ${shouldRegenerate ? 'replacement ' : ''}share link for "${note.title}".`
    );
    req.flash('success', `Share link ready: ${buildShareUrl(token)}`);
    await redirectWithSession(req, res, `/notes/${note.id}`);
  })
);

router.post(
  '/notes/:id/share-links/:shareLinkId/revoke',
  requireAuth,
  asyncHandler(async (req, res) => {
    const db = await getDb();
    const note = await getNoteById(Number(req.params.id));

    if (!note || note.user_id !== req.currentUser.id) {
      res.status(403).render('error', {
        pageTitle: 'Access denied',
        errorMessage: 'Only the note owner can revoke share links.'
      });
      return;
    }

    await db.run(
      `
        UPDATE share_links
        SET is_active = 0, revoked_at = CURRENT_TIMESTAMP
        WHERE id = ? AND note_id = ?
      `,
      [Number(req.params.shareLinkId), note.id]
    );

    await logActivity(req.currentUser.id, 'share_link_revoked', `Revoked share link for "${note.title}".`);
    req.flash('success', 'Share link revoked.');
    await redirectWithSession(req, res, `/notes/${note.id}`);
  })
);

router.post(
  '/notes/:id/rating',
  requireAuth,
  asyncHandler(async (req, res) => {
    const db = await getDb();
    const noteId = Number(req.params.id);
    const note = await getNoteById(noteId);
    const shareToken = req.query.shareToken || null;
    const errors = [];

    if (!note) {
      res.status(404).render('error', {
        pageTitle: 'Note not found',
        errorMessage: 'The requested note could not be found.'
      });
      return;
    }

    let canAccess = await userCanAccessNote(note, req.currentUser);

    if (!canAccess && shareToken) {
      const shareLink = await getActiveShareLink(shareToken);
      canAccess = Boolean(shareLink && shareLink.note_id === note.id);
    }

    if (!canAccess) {
      res.status(403).render('error', {
        pageTitle: 'Access denied',
        errorMessage: 'You do not have permission to rate this note.'
      });
      return;
    }

    const ratingValue = Number(req.body.ratingValue);
    const comment = (req.body.comment || '').trim();

    if (!Number.isInteger(ratingValue) || ratingValue < 1 || ratingValue > 5) {
      errors.push('Rating must be between 1 and 5 stars.');
    }

    if (comment.length > 1000) {
      errors.push('Comments must be 1000 characters or fewer.');
    }

    if (errors.length > 0) {
      const data = await loadNotePageData(note.id, req.currentUser);
      res.status(422).render('notes/show', {
        pageTitle: note.title,
        ...data,
        shareToken,
        shareUrlForToken: buildShareUrl,
        errors
      });
      return;
    }

    const existingRating = await db.get('SELECT id FROM ratings WHERE note_id = ? AND user_id = ?', [
      note.id,
      req.currentUser.id
    ]);

    if (existingRating) {
      await db.run(
        `
          UPDATE ratings
          SET rating_value = ?, comment = ?, updated_at = CURRENT_TIMESTAMP
          WHERE id = ?
        `,
        [ratingValue, comment, existingRating.id]
      );
    } else {
      await db.run(
        `
          INSERT INTO ratings (note_id, user_id, rating_value, comment)
          VALUES (?, ?, ?, ?)
        `,
        [note.id, req.currentUser.id, ratingValue, comment]
      );
    }

    await logActivity(req.currentUser.id, 'rating_saved', `Saved rating for "${note.title}".`);
    req.flash('success', 'Your rating has been saved.');
    await redirectWithSession(req, res, shareToken ? `/shared/${shareToken}` : `/notes/${note.id}`);
  })
);

router.get(
  '/attachments/:attachmentId/download',
  asyncHandler(async (req, res) => {
    const db = await getDb();
    const attachment = await db.get(
      `
        SELECT
          a.*,
          n.user_id,
          n.is_public
        FROM attachments a
        JOIN notes n ON n.id = a.note_id
        WHERE a.id = ?
      `,
      [Number(req.params.attachmentId)]
    );

    if (!attachment) {
      res.status(404).render('error', {
        pageTitle: 'Attachment not found',
        errorMessage: 'The requested attachment could not be found.'
      });
      return;
    }

    let canDownload = Boolean(attachment.is_public);

    if (req.currentUser) {
      canDownload =
        canDownload ||
        req.currentUser.id === attachment.user_id ||
        req.currentUser.role === 'Admin';
    }

    if (!canDownload && req.query.shareToken) {
      const shareLink = await getActiveShareLink(req.query.shareToken);
      canDownload = Boolean(shareLink && shareLink.note_id === attachment.note_id);
    }

    if (!canDownload) {
      res.status(403).render('error', {
        pageTitle: 'Access denied',
        errorMessage: 'You do not have permission to download this attachment.'
      });
      return;
    }

    res.download(
      `${config.uploadDir}\\${attachment.stored_filename}`,
      attachment.original_filename
    );
  })
);

module.exports = router;
