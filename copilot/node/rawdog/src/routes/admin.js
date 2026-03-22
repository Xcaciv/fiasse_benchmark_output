const express = require('express');
const { asyncHandler } = require('../lib/async-handler');
const { logActivity } = require('../lib/activity');
const { redirectWithSession } = require('../lib/auth');
const { getDb } = require('../db');
const { requireAdmin } = require('../middleware/auth');

const router = express.Router();

router.get(
  '/',
  requireAdmin,
  asyncHandler(async (req, res) => {
    const db = await getDb();
    const query = (req.query.q || '').trim();
    const wildcard = `%${query.toLowerCase()}%`;
    const totals = await db.get(
      `
        SELECT
          (SELECT COUNT(*) FROM users) AS user_count,
          (SELECT COUNT(*) FROM notes) AS note_count
      `
    );

    const users = await db.all(
      `
        SELECT
          u.id,
          u.username,
          u.email,
          u.role,
          u.created_at,
          COUNT(n.id) AS note_count
        FROM users u
        LEFT JOIN notes n ON n.user_id = u.id
        WHERE
          ? = ''
          OR LOWER(u.username) LIKE ?
          OR LOWER(u.email) LIKE ?
        GROUP BY u.id, u.username, u.email, u.role, u.created_at
        ORDER BY u.created_at DESC
      `,
      [query, wildcard, wildcard]
    );

    const recentLogs = await db.all(
      `
        SELECT
          al.*,
          u.username
        FROM activity_logs al
        LEFT JOIN users u ON u.id = al.user_id
        ORDER BY al.created_at DESC
        LIMIT 20
      `
    );

    const notes = await db.all(
      `
        SELECT
          n.id,
          n.title,
          n.is_public,
          n.created_at,
          owner.id AS owner_id,
          owner.username AS owner_username
        FROM notes n
        JOIN users owner ON owner.id = n.user_id
        ORDER BY n.created_at DESC
        LIMIT 30
      `
    );

    const assignableUsers = await db.all(
      `
        SELECT id, username, email
        FROM users
        ORDER BY username
      `
    );

    res.render('admin/dashboard', {
      pageTitle: 'Admin Dashboard',
      query,
      totals,
      users,
      recentLogs,
      notes,
      assignableUsers
    });
  })
);

router.post(
  '/notes/:id/reassign',
  requireAdmin,
  asyncHandler(async (req, res) => {
    const db = await getDb();
    const noteId = Number(req.params.id);
    const newOwnerId = Number(req.body.newOwnerId);

    const note = await db.get(
      `
        SELECT n.id, n.title, n.user_id, u.username AS owner_username
        FROM notes n
        JOIN users u ON u.id = n.user_id
        WHERE n.id = ?
      `,
      [noteId]
    );

    const newOwner = await db.get('SELECT id, username FROM users WHERE id = ?', [newOwnerId]);

    if (!note || !newOwner) {
      req.flash('error', 'The selected note or user could not be found.');
      await redirectWithSession(req, res, '/admin');
      return;
    }

    await db.run('UPDATE notes SET user_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?', [
      newOwner.id,
      note.id
    ]);

    await logActivity(
      req.currentUser.id,
      'admin_note_reassigned',
      `Admin reassigned note "${note.title}" from ${note.owner_username} to ${newOwner.username}.`
    );

    req.flash('success', `Reassigned "${note.title}" to ${newOwner.username}.`);
    await redirectWithSession(req, res, '/admin');
  })
);

module.exports = router;
