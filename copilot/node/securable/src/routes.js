'use strict';

const fs = require('node:fs');
const path = require('node:path');

const express = require('express');
const { body, validationResult } = require('express-validator');

const { dataDirectories, transaction } = require('./database');
const { verifyCsrfToken } = require('./middleware/csrf');
const { logActivity } = require('./services/activity-log-service');
const { sendPasswordResetEmail } = require('./services/mail-service');
const { addFlash } = require('./utils/flash');
const { createRandomToken, hashToken } = require('./utils/crypto');
const { hashPassword, isStrongPassword, verifyPassword } = require('./utils/passwords');
const { escapeLikeToken, makeExcerpt } = require('./utils/view-helpers');
const { upload } = require('./middleware/uploads');

function validateRequest(req) {
  const result = validationResult(req);
  if (result.isEmpty()) {
    return [];
  }

  return result.array().map((item) => item.msg);
}

function buildSearchQuery(searchTerm) {
  return `%${escapeLikeToken(searchTerm.toLowerCase())}%`;
}

function getHomeData(db, currentUserId, searchTerm = '') {
  const searchQuery = searchTerm ? buildSearchQuery(searchTerm) : null;
  const baseSql = `
    SELECT n.id, n.title, n.content, n.visibility, n.created_at, u.username AS author
    FROM notes n
    INNER JOIN users u ON u.id = n.user_id
    WHERE (
      n.visibility = 'public'
      OR n.user_id = ?
    )
    ${searchQuery ? `AND (LOWER(n.title) LIKE ? ESCAPE '\\' OR LOWER(n.content) LIKE ? ESCAPE '\\')` : ''}
    ORDER BY n.created_at DESC
  `;

  const anonymousSql = `
    SELECT n.id, n.title, n.content, n.visibility, n.created_at, u.username AS author
    FROM notes n
    INNER JOIN users u ON u.id = n.user_id
    WHERE n.visibility = 'public'
    ${searchQuery ? `AND (LOWER(n.title) LIKE ? ESCAPE '\\' OR LOWER(n.content) LIKE ? ESCAPE '\\')` : ''}
    ORDER BY n.created_at DESC
  `;

  const statement = currentUserId ? db.prepare(baseSql) : db.prepare(anonymousSql);
  const rows = currentUserId
    ? statement.all(currentUserId, ...(searchQuery ? [searchQuery, searchQuery] : []))
    : statement.all(...(searchQuery ? [searchQuery, searchQuery] : []));

  return rows.map((row) => ({
    ...row,
    excerpt: makeExcerpt(row.content)
  }));
}

function getDashboardNotes(db, userId) {
  return db
    .prepare(
      `SELECT n.id, n.title, n.visibility, n.created_at, n.updated_at,
              COUNT(DISTINCT a.id) AS attachment_count,
              COUNT(DISTINCT r.id) AS rating_count
       FROM notes n
       LEFT JOIN attachments a ON a.note_id = n.id
       LEFT JOIN ratings r ON r.note_id = n.id
       WHERE n.user_id = ?
       GROUP BY n.id
       ORDER BY n.updated_at DESC`
    )
    .all(userId);
}

function getNoteById(db, noteId) {
  return db
    .prepare(
      `SELECT n.*, u.username AS author, u.email AS author_email
       FROM notes n
       INNER JOIN users u ON u.id = n.user_id
       WHERE n.id = ?`
    )
    .get(noteId);
}

function getAttachmentsForNote(db, noteId) {
  return db
    .prepare(
      `SELECT id, note_id, original_name, mime_type, file_size, created_at
       FROM attachments
       WHERE note_id = ?
       ORDER BY created_at ASC`
    )
    .all(noteId);
}

function getRatingsForNote(db, noteId) {
  return db
    .prepare(
      `SELECT r.id, r.value, r.comment, r.created_at, r.updated_at, u.username AS rater_username, r.user_id
       FROM ratings r
       INNER JOIN users u ON u.id = r.user_id
       WHERE r.note_id = ?
       ORDER BY r.created_at DESC`
    )
    .all(noteId);
}

function getRatingSummary(db, noteId) {
  return db
    .prepare(
      `SELECT ROUND(AVG(value), 2) AS average_rating, COUNT(*) AS rating_count
       FROM ratings
       WHERE note_id = ?`
    )
    .get(noteId);
}

function getUserRating(db, noteId, userId) {
  if (!userId) {
    return null;
  }

  return db
    .prepare('SELECT id, value, comment FROM ratings WHERE note_id = ? AND user_id = ?')
    .get(noteId, userId);
}

function canViewNote(note, user) {
  if (!note) {
    return false;
  }

  if (note.visibility === 'public') {
    return true;
  }

  if (!user) {
    return false;
  }

  return user.role === 'admin' || note.user_id === user.id;
}

function canManageNote(note, user) {
  return Boolean(user) && (user.role === 'admin' || note.user_id === user.id);
}

function renderNotePage(req, res, db, note, options = {}) {
  const attachments = getAttachmentsForNote(db, note.id);
  const ratings = getRatingsForNote(db, note.id);
  const ratingSummary = getRatingSummary(db, note.id);
  const ownRating = req.currentUser ? getUserRating(db, note.id, req.currentUser.id) : null;
  const activeShareLink = db
    .prepare('SELECT id, created_at FROM share_links WHERE note_id = ? AND revoked_at IS NULL')
    .get(note.id);

  return res.render('notes/show', {
    title: note.title,
    note,
    attachments,
    canManage: canManageNote(note, req.currentUser),
    canRate: Boolean(req.currentUser),
    currentRating: ownRating,
    ratingSummary,
    ratings,
    activeShareLink,
    shareUrl: options.shareUrl || null,
    sharedMode: Boolean(options.sharedMode),
    sharedToken: options.sharedToken || null
  });
}

function removeStoredFiles(attachments) {
  for (const attachment of attachments) {
    const filePath = path.join(dataDirectories.uploads, attachment.storage_name);
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }
  }
}

function createAppRouter({ db, logger, requireAuth, requireAdmin }) {
  const router = express.Router();

  const registrationValidators = [
    body('username').trim().isLength({ min: 3, max: 30 }).withMessage('Username must be 3-30 characters long.'),
    body('username').matches(/^[A-Za-z0-9_-]+$/).withMessage('Username can use letters, numbers, underscores, and dashes only.'),
    body('email').trim().isEmail().withMessage('Enter a valid email address.').normalizeEmail(),
    body('password').custom((value) => {
      if (!isStrongPassword(value)) {
        throw new Error('Password must be at least 12 characters and include upper, lower, number, and symbol characters.');
      }
      return true;
    }),
    body('confirmPassword').custom((value, { req }) => {
      if (value !== req.body.password) {
        throw new Error('Password confirmation must match.');
      }
      return true;
    })
  ];

  const noteValidators = [
    body('title').trim().isLength({ min: 1, max: 120 }).withMessage('Title is required and must be at most 120 characters.'),
    body('content').trim().isLength({ min: 1, max: 10000 }).withMessage('Content is required and must be at most 10,000 characters.'),
    body('visibility').isIn(['private', 'public']).withMessage('Visibility must be public or private.')
  ];

  const ratingValidators = [
    body('value').isInt({ min: 1, max: 5 }).withMessage('Rating must be between 1 and 5.'),
    body('comment').optional({ values: 'falsy' }).trim().isLength({ max: 1000 }).withMessage('Comment must be at most 1,000 characters.')
  ];

  const profileValidators = [
    body('username').trim().isLength({ min: 3, max: 30 }).withMessage('Username must be 3-30 characters long.'),
    body('username').matches(/^[A-Za-z0-9_-]+$/).withMessage('Username can use letters, numbers, underscores, and dashes only.'),
    body('email').trim().isEmail().withMessage('Enter a valid email address.').normalizeEmail()
  ];

  router.get('/', (req, res) => {
    const searchTerm = (req.query.q || '').trim();
    const notes = getHomeData(db, req.currentUser ? req.currentUser.id : null, searchTerm);
    res.render('home', {
      title: 'Search notes',
      notes,
      searchTerm
    });
  });

  router.get('/top-rated', (req, res) => {
    const notes = db
      .prepare(
        `SELECT n.id, n.title, n.content, u.username AS author,
                ROUND(AVG(r.value), 2) AS average_rating,
                COUNT(r.id) AS rating_count
         FROM notes n
         INNER JOIN users u ON u.id = n.user_id
         INNER JOIN ratings r ON r.note_id = n.id
         WHERE n.visibility = 'public'
         GROUP BY n.id
         HAVING COUNT(r.id) >= 3
         ORDER BY average_rating DESC, rating_count DESC, n.created_at DESC`
      )
      .all()
      .map((note) => ({
        ...note,
        excerpt: makeExcerpt(note.content)
      }));

    res.render('top-rated', {
      title: 'Top rated notes',
      notes
    });
  });

  router.get('/register', (req, res) => {
    res.render('auth/register', {
      title: 'Register',
      form: {}
    });
  });

  router.post('/register', verifyCsrfToken, registrationValidators, async (req, res) => {
    const errors = validateRequest(req);
    if (errors.length) {
      return res.status(400).render('auth/register', {
        title: 'Register',
        errors,
        form: req.body
      });
    }

    const existingUser = db
      .prepare('SELECT id FROM users WHERE username = ? OR email = ?')
      .get(req.body.username.trim(), req.body.email.trim().toLowerCase());

    if (existingUser) {
      return res.status(400).render('auth/register', {
        title: 'Register',
        errors: ['That username or email address is already in use.'],
        form: req.body
      });
    }

    const passwordHash = await hashPassword(req.body.password);
    const result = db
      .prepare('INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)')
      .run(req.body.username.trim(), req.body.email.trim().toLowerCase(), passwordHash);

    req.session.userId = result.lastInsertRowid;
    logActivity(db, result.lastInsertRowid, 'user.registered', 'user', result.lastInsertRowid, {
      username: req.body.username.trim()
    });
    addFlash(req, 'success', 'Welcome to Loose Notes. Your account has been created.');
    return res.redirect('/dashboard');
  });

  router.get('/login', (req, res) => {
    res.render('auth/login', {
      title: 'Login',
      form: {}
    });
  });

  router.post(
    '/login',
    [
      body('username').trim().notEmpty().withMessage('Enter your username.'),
      body('password').notEmpty().withMessage('Enter your password.')
    ],
    verifyCsrfToken,
    async (req, res) => {
      const errors = validateRequest(req);
      if (errors.length) {
        return res.status(400).render('auth/login', {
          title: 'Login',
          errors,
          form: req.body
        });
      }

      const user = db
        .prepare('SELECT id, username, email, role, password_hash FROM users WHERE username = ?')
        .get(req.body.username.trim());

      if (!user || !(await verifyPassword(req.body.password, user.password_hash))) {
        logActivity(db, user ? user.id : null, 'auth.login_failed', 'user', user ? user.id : null, {
          username: req.body.username.trim()
        });
        return res.status(400).render('auth/login', {
          title: 'Login',
          errors: ['Invalid username or password.'],
          form: { username: req.body.username }
        });
      }

      req.session.userId = user.id;
      logActivity(db, user.id, 'auth.login_succeeded', 'user', user.id, {});
      addFlash(req, 'success', `Welcome back, ${user.username}.`);
      return res.redirect('/dashboard');
    }
  );

  router.post('/logout', requireAuth, verifyCsrfToken, (req, res, next) => {
    const userId = req.currentUser.id;
    req.session.destroy((error) => {
      if (error) {
        return next(error);
      }

      logActivity(db, userId, 'auth.logout', 'user', userId, {});
      return res.redirect('/');
    });
  });

  router.get('/forgot-password', (req, res) => {
    res.render('auth/request-reset', {
      title: 'Reset password'
    });
  });

  router.post(
    '/forgot-password',
    [body('email').trim().isEmail().withMessage('Enter a valid email address.').normalizeEmail()],
    verifyCsrfToken,
    async (req, res, next) => {
      const errors = validateRequest(req);
      if (errors.length) {
        return res.status(400).render('auth/request-reset', {
          title: 'Reset password',
          errors
        });
      }

      const user = db
        .prepare('SELECT id, username, email FROM users WHERE email = ?')
        .get(req.body.email.trim().toLowerCase());

      if (user) {
        const rawToken = createRandomToken(32);
        const tokenHash = hashToken(rawToken);
        const expiresAt = new Date(Date.now() + 60 * 60 * 1000).toISOString();

        db.prepare('UPDATE password_reset_tokens SET used_at = CURRENT_TIMESTAMP WHERE user_id = ? AND used_at IS NULL').run(user.id);
        db.prepare(
          'INSERT INTO password_reset_tokens (user_id, token_hash, expires_at) VALUES (?, ?, ?)'
        ).run(user.id, tokenHash, expiresAt);

        const resetUrl = `${req.protocol}://${req.get('host')}/reset-password/${rawToken}`;
        try {
          const mailFilePath = await sendPasswordResetEmail({
            to: user.email,
            username: user.username,
            resetUrl
          });
          logActivity(db, user.id, 'auth.password_reset_requested', 'user', user.id, {
            email: user.email,
            mailFilePath
          });
        } catch (error) {
          logger.error('Failed to write password reset email', {
            message: error.message,
            stack: error.stack
          });
          return next(error);
        }
      }

      addFlash(req, 'info', 'If the email address exists, a reset link has been generated and written to the local mail outbox.');
      return res.redirect('/login');
    }
  );

  router.get('/reset-password/:token', (req, res) => {
    const tokenHash = hashToken(req.params.token);
    const resetToken = db
      .prepare(
        `SELECT prt.id, prt.user_id, prt.expires_at, prt.used_at, u.username
         FROM password_reset_tokens prt
         INNER JOIN users u ON u.id = prt.user_id
         WHERE prt.token_hash = ?`
      )
      .get(tokenHash);

    const tokenExpired = !resetToken || resetToken.used_at || new Date(resetToken.expires_at) < new Date();
    if (tokenExpired) {
      return res.status(400).render('auth/reset-password', {
        title: 'Reset password',
        invalidToken: true
      });
    }

    return res.render('auth/reset-password', {
      title: 'Reset password',
      invalidToken: false,
      token: req.params.token,
      username: resetToken.username
    });
  });

  router.post(
    '/reset-password/:token',
    [
      body('password').custom((value) => {
        if (!isStrongPassword(value)) {
          throw new Error('Password must be at least 12 characters and include upper, lower, number, and symbol characters.');
        }
        return true;
      }),
      body('confirmPassword').custom((value, { req }) => {
        if (value !== req.body.password) {
          throw new Error('Password confirmation must match.');
        }
        return true;
      })
    ],
    verifyCsrfToken,
    async (req, res) => {
      const errors = validateRequest(req);
      const tokenHash = hashToken(req.params.token);
      const resetToken = db
        .prepare(
          `SELECT id, user_id, expires_at, used_at
           FROM password_reset_tokens
           WHERE token_hash = ?`
        )
        .get(tokenHash);

      const tokenExpired = !resetToken || resetToken.used_at || new Date(resetToken.expires_at) < new Date();
      if (tokenExpired) {
        return res.status(400).render('auth/reset-password', {
          title: 'Reset password',
          invalidToken: true
        });
      }

      if (errors.length) {
        return res.status(400).render('auth/reset-password', {
          title: 'Reset password',
          invalidToken: false,
          token: req.params.token,
          errors
        });
      }

      const passwordHash = await hashPassword(req.body.password);
      db.prepare('UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?').run(passwordHash, resetToken.user_id);
      db.prepare('UPDATE password_reset_tokens SET used_at = CURRENT_TIMESTAMP WHERE id = ?').run(resetToken.id);
      logActivity(db, resetToken.user_id, 'auth.password_reset_completed', 'user', resetToken.user_id, {});
      addFlash(req, 'success', 'Your password has been reset. Sign in with your new password.');
      return res.redirect('/login');
    }
  );

  router.get('/dashboard', requireAuth, (req, res) => {
    res.render('dashboard', {
      title: 'Your notes',
      notes: getDashboardNotes(db, req.currentUser.id)
    });
  });

  router.get('/notes/new', requireAuth, (req, res) => {
    res.render('notes/form', {
      title: 'Create note',
      formAction: '/notes',
      note: { visibility: 'private' },
      errors: []
    });
  });

  router.post('/notes', requireAuth, verifyCsrfToken, upload.array('attachments', 5), noteValidators, (req, res, next) => {
    try {
      const errors = validateRequest(req);
      if (errors.length) {
        removeStoredFiles((req.files || []).map((file) => ({ storage_name: file.filename })));
        return res.status(400).render('notes/form', {
          title: 'Create note',
          formAction: '/notes',
          note: req.body,
          errors
        });
      }

      const createNoteTransaction = transaction((payload) => {
        const noteResult = db
          .prepare(
            `INSERT INTO notes (user_id, title, content, visibility)
             VALUES (?, ?, ?, ?)`
          )
          .run(payload.userId, payload.title, payload.content, payload.visibility);

        for (const file of payload.files) {
          db.prepare(
            `INSERT INTO attachments (note_id, storage_name, original_name, mime_type, file_size)
             VALUES (?, ?, ?, ?, ?)`
          ).run(noteResult.lastInsertRowid, file.filename, file.originalname, file.mimetype, file.size);
        }

        return noteResult.lastInsertRowid;
      });

      const noteId = createNoteTransaction({
        userId: req.currentUser.id,
        title: req.body.title.trim(),
        content: req.body.content.trim(),
        visibility: req.body.visibility,
        files: req.files || []
      });

      logActivity(db, req.currentUser.id, 'note.created', 'note', noteId, {
        visibility: req.body.visibility,
        attachmentCount: (req.files || []).length
      });
      addFlash(req, 'success', 'Your note was created.');
      return res.redirect(`/notes/${noteId}`);
    } catch (error) {
      return next(error);
    }
  });

  router.get('/notes/:id', (req, res) => {
    const note = getNoteById(db, req.params.id);
    if (!canViewNote(note, req.currentUser)) {
      return res.status(404).render('error', {
        title: 'Note not available',
        heading: 'Note not available',
        message: 'That note is not available to you.'
      });
    }

    return renderNotePage(req, res, db, note);
  });

  router.get('/notes/:id/edit', requireAuth, (req, res) => {
    const note = getNoteById(db, req.params.id);
    if (!note || !canManageNote(note, req.currentUser)) {
      addFlash(req, 'danger', 'You are not allowed to edit that note.');
      return res.redirect('/dashboard');
    }

    return res.render('notes/form', {
      title: 'Edit note',
      formAction: `/notes/${note.id}/edit`,
      note,
      attachments: getAttachmentsForNote(db, note.id),
      errors: []
    });
  });

  router.post('/notes/:id/edit', requireAuth, verifyCsrfToken, upload.array('attachments', 5), noteValidators, (req, res, next) => {
    try {
      const note = getNoteById(db, req.params.id);
      if (!note || !canManageNote(note, req.currentUser)) {
        removeStoredFiles((req.files || []).map((file) => ({ storage_name: file.filename })));
        addFlash(req, 'danger', 'You are not allowed to edit that note.');
        return res.redirect('/dashboard');
      }

      const errors = validateRequest(req);
      if (errors.length) {
        removeStoredFiles((req.files || []).map((file) => ({ storage_name: file.filename })));
        return res.status(400).render('notes/form', {
          title: 'Edit note',
          formAction: `/notes/${note.id}/edit`,
          note: { ...note, ...req.body },
          attachments: getAttachmentsForNote(db, note.id),
          errors
        });
      }

      const editTransaction = transaction((payload) => {
        db.prepare(
          `UPDATE notes
           SET title = ?, content = ?, visibility = ?, updated_at = CURRENT_TIMESTAMP
           WHERE id = ?`
        ).run(payload.title, payload.content, payload.visibility, payload.noteId);

        for (const file of payload.files) {
          db.prepare(
            `INSERT INTO attachments (note_id, storage_name, original_name, mime_type, file_size)
             VALUES (?, ?, ?, ?, ?)`
          ).run(payload.noteId, file.filename, file.originalname, file.mimetype, file.size);
        }
      });

      editTransaction({
        noteId: note.id,
        title: req.body.title.trim(),
        content: req.body.content.trim(),
        visibility: req.body.visibility,
        files: req.files || []
      });

      logActivity(db, req.currentUser.id, 'note.updated', 'note', note.id, {
        visibility: req.body.visibility,
        attachmentCountAdded: (req.files || []).length
      });
      addFlash(req, 'success', 'Your note was updated.');
      return res.redirect(`/notes/${note.id}`);
    } catch (error) {
      return next(error);
    }
  });

  router.get('/notes/:id/delete', requireAuth, (req, res) => {
    const note = getNoteById(db, req.params.id);
    if (!note || !canManageNote(note, req.currentUser)) {
      addFlash(req, 'danger', 'You are not allowed to delete that note.');
      return res.redirect('/dashboard');
    }

    return res.render('notes/delete', {
      title: 'Delete note',
      note
    });
  });

  router.post('/notes/:id/delete', requireAuth, verifyCsrfToken, (req, res) => {
    const note = getNoteById(db, req.params.id);
    if (!note || !canManageNote(note, req.currentUser)) {
      addFlash(req, 'danger', 'You are not allowed to delete that note.');
      return res.redirect('/dashboard');
    }

    const attachments = db
      .prepare('SELECT storage_name FROM attachments WHERE note_id = ?')
      .all(note.id);

    const deleteTransaction = transaction((noteId) => {
      db.prepare('DELETE FROM notes WHERE id = ?').run(noteId);
    });

    deleteTransaction(note.id);
    removeStoredFiles(attachments);
    logActivity(db, req.currentUser.id, 'note.deleted', 'note', note.id, {
      noteOwnerId: note.user_id
    });
    addFlash(req, 'success', 'The note and related data were deleted.');
    return res.redirect('/dashboard');
  });

  router.post('/notes/:id/share', requireAuth, verifyCsrfToken, (req, res) => {
    const note = getNoteById(db, req.params.id);
    if (!note || note.user_id !== req.currentUser.id) {
      addFlash(req, 'danger', 'Only the note owner can manage share links.');
      return res.redirect('/dashboard');
    }

    const rawToken = createRandomToken(32);
    const tokenHash = hashToken(rawToken);
    const shareTransaction = transaction((noteId) => {
      db.prepare('UPDATE share_links SET revoked_at = CURRENT_TIMESTAMP WHERE note_id = ? AND revoked_at IS NULL').run(noteId);
      db.prepare('INSERT INTO share_links (note_id, token_hash) VALUES (?, ?)').run(noteId, tokenHash);
    });

    shareTransaction(note.id);
    const shareUrl = `${req.protocol}://${req.get('host')}/shared/${rawToken}`;
    logActivity(db, req.currentUser.id, 'note.share_link_generated', 'note', note.id, {});
    addFlash(req, 'success', 'A new share link has been generated.', { shareUrl });
    return res.redirect(`/notes/${note.id}`);
  });

  router.post('/notes/:id/share/revoke', requireAuth, verifyCsrfToken, (req, res) => {
    const note = getNoteById(db, req.params.id);
    if (!note || note.user_id !== req.currentUser.id) {
      addFlash(req, 'danger', 'Only the note owner can manage share links.');
      return res.redirect('/dashboard');
    }

    db.prepare('UPDATE share_links SET revoked_at = CURRENT_TIMESTAMP WHERE note_id = ? AND revoked_at IS NULL').run(note.id);
    logActivity(db, req.currentUser.id, 'note.share_link_revoked', 'note', note.id, {});
    addFlash(req, 'success', 'Active share links have been revoked.');
    return res.redirect(`/notes/${note.id}`);
  });

  router.get('/shared/:token', (req, res) => {
    const tokenHash = hashToken(req.params.token);
    const sharedNote = db
      .prepare(
        `SELECT n.*, u.username AS author, sl.id AS share_link_id
         FROM share_links sl
         INNER JOIN notes n ON n.id = sl.note_id
         INNER JOIN users u ON u.id = n.user_id
         WHERE sl.token_hash = ? AND sl.revoked_at IS NULL`
      )
      .get(tokenHash);

    if (!sharedNote) {
      return res.status(404).render('error', {
        title: 'Share link not found',
        heading: 'Share link not found',
        message: 'That share link is invalid or has been revoked.'
      });
    }

    return renderNotePage(req, res, db, sharedNote, {
      sharedMode: true,
      sharedToken: req.params.token
    });
  });

  router.get('/attachments/:id', (req, res) => {
    const attachment = db
      .prepare(
        `SELECT a.*, n.user_id, n.visibility
         FROM attachments a
         INNER JOIN notes n ON n.id = a.note_id
         WHERE a.id = ?`
      )
      .get(req.params.id);

    if (!attachment || !canViewNote(attachment, req.currentUser)) {
      return res.status(404).render('error', {
        title: 'Attachment not available',
        heading: 'Attachment not available',
        message: 'That attachment is not available to you.'
      });
    }

    const filePath = path.join(dataDirectories.uploads, attachment.storage_name);
    return res.download(filePath, attachment.original_name);
  });

  router.get('/shared/:token/attachments/:id', (req, res) => {
    const tokenHash = hashToken(req.params.token);
    const attachment = db
      .prepare(
        `SELECT a.*, sl.id AS share_link_id
         FROM share_links sl
         INNER JOIN notes n ON n.id = sl.note_id
         INNER JOIN attachments a ON a.note_id = n.id
         WHERE sl.token_hash = ? AND sl.revoked_at IS NULL AND a.id = ?`
      )
      .get(tokenHash, req.params.id);

    if (!attachment) {
      return res.status(404).render('error', {
        title: 'Attachment not available',
        heading: 'Attachment not available',
        message: 'That shared attachment is not available.'
      });
    }

    const filePath = path.join(dataDirectories.uploads, attachment.storage_name);
    return res.download(filePath, attachment.original_name);
  });

  router.post('/notes/:id/ratings', requireAuth, verifyCsrfToken, ratingValidators, (req, res) => {
    const note = getNoteById(db, req.params.id);
    if (!canViewNote(note, req.currentUser)) {
      addFlash(req, 'danger', 'You cannot rate that note.');
      return res.redirect('/');
    }

    const errors = validateRequest(req);
    if (errors.length) {
      addFlash(req, 'danger', errors[0]);
      return res.redirect(req.get('referer') || `/notes/${note.id}`);
    }

    const upsertRating = db.prepare(
      `INSERT INTO ratings (note_id, user_id, value, comment)
       VALUES (?, ?, ?, ?)
       ON CONFLICT(note_id, user_id)
       DO UPDATE SET value = excluded.value, comment = excluded.comment, updated_at = CURRENT_TIMESTAMP`
    );

    upsertRating.run(note.id, req.currentUser.id, Number(req.body.value), (req.body.comment || '').trim() || null);
    logActivity(db, req.currentUser.id, 'note.rating_saved', 'note', note.id, {
      value: Number(req.body.value)
    });
    addFlash(req, 'success', 'Your rating has been saved.');
    return res.redirect(req.get('referer') || `/notes/${note.id}`);
  });

  router.get('/profile', requireAuth, (req, res) => {
    res.render('profile', {
      title: 'Your profile',
      form: req.currentUser
    });
  });

  router.post('/profile', requireAuth, verifyCsrfToken, profileValidators, (req, res) => {
    const errors = validateRequest(req);
    if (errors.length) {
      return res.status(400).render('profile', {
        title: 'Your profile',
        form: req.body,
        errors
      });
    }

    const existingUser = db
      .prepare('SELECT id FROM users WHERE (username = ? OR email = ?) AND id <> ?')
      .get(req.body.username.trim(), req.body.email.trim().toLowerCase(), req.currentUser.id);

    if (existingUser) {
      return res.status(400).render('profile', {
        title: 'Your profile',
        form: req.body,
        errors: ['That username or email address is already in use.']
      });
    }

    db.prepare(
      `UPDATE users
       SET username = ?, email = ?, updated_at = CURRENT_TIMESTAMP
       WHERE id = ?`
    ).run(req.body.username.trim(), req.body.email.trim().toLowerCase(), req.currentUser.id);
    logActivity(db, req.currentUser.id, 'user.profile_updated', 'user', req.currentUser.id, {});
    addFlash(req, 'success', 'Your profile details have been updated.');
    return res.redirect('/profile');
  });

  router.post(
    '/profile/password',
    requireAuth,
    verifyCsrfToken,
    [
      body('currentPassword').notEmpty().withMessage('Enter your current password.'),
      body('newPassword').custom((value) => {
        if (!isStrongPassword(value)) {
          throw new Error('New password must be at least 12 characters and include upper, lower, number, and symbol characters.');
        }
        return true;
      }),
      body('confirmPassword').custom((value, { req }) => {
        if (value !== req.body.newPassword) {
          throw new Error('Password confirmation must match.');
        }
        return true;
      })
    ],
    async (req, res) => {
      const errors = validateRequest(req);
      if (errors.length) {
        return res.status(400).render('profile', {
          title: 'Your profile',
          form: req.currentUser,
          passwordErrors: errors
        });
      }

      const user = db
        .prepare('SELECT id, password_hash FROM users WHERE id = ?')
        .get(req.currentUser.id);

      const validCurrentPassword = await verifyPassword(req.body.currentPassword, user.password_hash);
      if (!validCurrentPassword) {
        return res.status(400).render('profile', {
          title: 'Your profile',
          form: req.currentUser,
          passwordErrors: ['Your current password is incorrect.']
        });
      }

      const passwordHash = await hashPassword(req.body.newPassword);
      db.prepare('UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?').run(passwordHash, req.currentUser.id);
      logActivity(db, req.currentUser.id, 'user.password_changed', 'user', req.currentUser.id, {});
      addFlash(req, 'success', 'Your password has been updated.');
      return res.redirect('/profile');
    }
  );

  router.get('/admin', requireAdmin, (req, res) => {
    const totals = db
      .prepare(
        `SELECT
           (SELECT COUNT(*) FROM users) AS user_count,
           (SELECT COUNT(*) FROM notes) AS note_count`
      )
      .get();

    const recentActivity = db
      .prepare(
        `SELECT al.*, u.username AS actor_username
         FROM activity_logs al
         LEFT JOIN users u ON u.id = al.actor_user_id
         ORDER BY al.created_at DESC
         LIMIT 20`
      )
      .all();

    res.render('admin/dashboard', {
      title: 'Admin dashboard',
      totals,
      recentActivity
    });
  });

  router.get('/admin/users', requireAdmin, (req, res) => {
    const searchTerm = (req.query.q || '').trim().toLowerCase();
    const users = db
      .prepare(
        `SELECT u.id, u.username, u.email, u.role, u.created_at, COUNT(n.id) AS note_count
         FROM users u
         LEFT JOIN notes n ON n.user_id = u.id
         WHERE (? = '' OR LOWER(u.username) LIKE ? OR LOWER(u.email) LIKE ?)
         GROUP BY u.id
         ORDER BY u.created_at DESC`
      )
      .all(searchTerm, `%${searchTerm}%`, `%${searchTerm}%`);

    res.render('admin/users', {
      title: 'User management',
      users,
      searchTerm
    });
  });

  router.get('/admin/notes/:id/reassign', requireAdmin, (req, res) => {
    const note = getNoteById(db, req.params.id);
    if (!note) {
      return res.status(404).render('error', {
        title: 'Note not found',
        heading: 'Note not found',
        message: 'The selected note does not exist.'
      });
    }

    const users = db
      .prepare('SELECT id, username, email FROM users WHERE id <> ? ORDER BY username ASC')
      .all(note.user_id);

    res.render('admin/reassign', {
      title: 'Reassign note ownership',
      note,
      users
    });
  });

  router.post(
    '/admin/notes/:id/reassign',
    requireAdmin,
    verifyCsrfToken,
    [body('newOwnerId').isInt({ min: 1 }).withMessage('Select a valid user.')],
    (req, res) => {
      const note = getNoteById(db, req.params.id);
      if (!note) {
        return res.status(404).render('error', {
          title: 'Note not found',
          heading: 'Note not found',
          message: 'The selected note does not exist.'
        });
      }

      const errors = validateRequest(req);
      if (errors.length) {
        addFlash(req, 'danger', errors[0]);
        return res.redirect(`/admin/notes/${note.id}/reassign`);
      }

      const newOwner = db
        .prepare('SELECT id, username FROM users WHERE id = ?')
        .get(Number(req.body.newOwnerId));

      if (!newOwner) {
        addFlash(req, 'danger', 'The selected user does not exist.');
        return res.redirect(`/admin/notes/${note.id}/reassign`);
      }

      db.prepare('UPDATE notes SET user_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?').run(newOwner.id, note.id);
      logActivity(db, req.currentUser.id, 'admin.note_reassigned', 'note', note.id, {
        previousOwnerId: note.user_id,
        newOwnerId: newOwner.id
      });
      addFlash(req, 'success', `Note ownership was reassigned to ${newOwner.username}.`);
      return res.redirect(`/notes/${note.id}`);
    }
  );

  return router;
}

module.exports = { createAppRouter };
