'use strict';

const { Op, fn, col, literal } = require('sequelize');
const { Note, User, Rating } = require('../models');

const DIALECT = process.env.DATABASE_URL && process.env.DATABASE_URL.startsWith('postgres') ? 'postgres' : 'sqlite';
const likeOp = DIALECT === 'postgres' ? Op.iLike : Op.like;

async function getSearchResults(req, res, next) {
  try {
    const query = (req.query.q || '').trim().slice(0, 200);
    if (!query) {
      return res.render('search/results', { title: 'Search', query: '', results: [], csrfToken: req.csrfToken() });
    }
    const pattern = `%${query}%`;
    const whereClause = {
      [Op.or]: [
        { title: { [likeOp]: pattern } },
        { content: { [likeOp]: pattern } },
      ],
      [Op.and]: [
        {
          [Op.or]: [
            { userId: req.user.id },
            { visibility: 'public' },
          ],
        },
      ],
    };
    const notes = await Note.findAll({
      where: whereClause,
      include: [
        { model: User, as: 'author', attributes: ['username'] },
        { model: Rating, attributes: ['value'] },
      ],
      order: [['updatedAt', 'DESC']],
      limit: 50,
    });

    const results = notes.map((note) => {
      const avg = note.Ratings.length
        ? (note.Ratings.reduce((s, r) => s + r.value, 0) / note.Ratings.length).toFixed(1)
        : null;
      return {
        id: note.id,
        title: note.title,
        excerpt: note.content.slice(0, 200),
        author: note.author ? note.author.username : 'Unknown',
        createdAt: note.createdAt,
        avgRating: avg,
        ratingCount: note.Ratings.length,
      };
    });

    return res.render('search/results', { title: 'Search Results', query, results, csrfToken: req.csrfToken() });
  } catch (err) {
    return next(err);
  }
}

module.exports = { getSearchResults };
