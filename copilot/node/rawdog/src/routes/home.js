const express = require('express');
const { asyncHandler } = require('../lib/async-handler');
const { getDb } = require('../db');

const router = express.Router();

router.get(
  '/',
  asyncHandler(async (req, res) => {
    const db = await getDb();
    const query = (req.query.q || '').trim();
    let searchResults = [];

    if (query) {
      const wildcard = `%${query.toLowerCase()}%`;

      if (req.currentUser) {
        searchResults = await db.all(
          `
            SELECT
              n.*,
              u.username AS author_username,
              (
                SELECT ROUND(AVG(r.rating_value), 2)
                FROM ratings r
                WHERE r.note_id = n.id
              ) AS average_rating,
              (
                SELECT COUNT(*)
                FROM ratings r
                WHERE r.note_id = n.id
              ) AS rating_count
            FROM notes n
            JOIN users u ON u.id = n.user_id
            WHERE
              (LOWER(n.title) LIKE ? OR LOWER(n.content) LIKE ?)
              AND (n.is_public = 1 OR n.user_id = ?)
            ORDER BY
              CASE WHEN n.user_id = ? THEN 0 ELSE 1 END,
              n.created_at DESC
          `,
          [wildcard, wildcard, req.currentUser.id, req.currentUser.id]
        );
      } else {
        searchResults = await db.all(
          `
            SELECT
              n.*,
              u.username AS author_username,
              (
                SELECT ROUND(AVG(r.rating_value), 2)
                FROM ratings r
                WHERE r.note_id = n.id
              ) AS average_rating,
              (
                SELECT COUNT(*)
                FROM ratings r
                WHERE r.note_id = n.id
              ) AS rating_count
            FROM notes n
            JOIN users u ON u.id = n.user_id
            WHERE
              n.is_public = 1
              AND (LOWER(n.title) LIKE ? OR LOWER(n.content) LIKE ?)
            ORDER BY n.created_at DESC
          `,
          [wildcard, wildcard]
        );
      }
    }

    const recentPublicNotes = await db.all(
      `
        SELECT
          n.*,
          u.username AS author_username,
          (
            SELECT ROUND(AVG(r.rating_value), 2)
            FROM ratings r
            WHERE r.note_id = n.id
          ) AS average_rating,
          (
            SELECT COUNT(*)
            FROM ratings r
            WHERE r.note_id = n.id
          ) AS rating_count
        FROM notes n
        JOIN users u ON u.id = n.user_id
        WHERE n.is_public = 1
        ORDER BY n.created_at DESC
        LIMIT 8
      `
    );

    const recentOwnNotes = req.currentUser
      ? await db.all(
          `
            SELECT
              n.*,
              (
                SELECT ROUND(AVG(r.rating_value), 2)
                FROM ratings r
                WHERE r.note_id = n.id
              ) AS average_rating,
              (
                SELECT COUNT(*)
                FROM ratings r
                WHERE r.note_id = n.id
              ) AS rating_count
            FROM notes n
            WHERE n.user_id = ?
            ORDER BY n.updated_at DESC
            LIMIT 8
          `,
          [req.currentUser.id]
        )
      : [];

    res.render('home/index', {
      pageTitle: 'Loose Notes',
      query,
      searchResults,
      recentPublicNotes,
      recentOwnNotes
    });
  })
);

router.get(
  '/top-rated',
  asyncHandler(async (req, res) => {
    const db = await getDb();
    const notes = await db.all(
      `
        SELECT
          n.id,
          n.title,
          n.content,
          n.created_at,
          u.username AS author_username,
          ROUND(AVG(r.rating_value), 2) AS average_rating,
          COUNT(r.id) AS rating_count
        FROM notes n
        JOIN users u ON u.id = n.user_id
        JOIN ratings r ON r.note_id = n.id
        WHERE n.is_public = 1
        GROUP BY n.id, n.title, n.content, n.created_at, u.username
        HAVING COUNT(r.id) >= 3
        ORDER BY average_rating DESC, rating_count DESC, n.created_at DESC
      `
    );

    res.render('home/top-rated', {
      pageTitle: 'Top Rated Notes',
      notes
    });
  })
);

module.exports = router;
