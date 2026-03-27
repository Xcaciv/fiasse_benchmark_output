'use strict';

const express = require('express');
const router = express.Router();
const { Note, User, Rating } = require('../models');
const { fn, col, literal } = require('sequelize');
const requireAuth = require('../middleware/requireAuth');

router.use('/auth', require('./auth'));
router.use('/notes/:id/attachments', require('./attachments'));
router.use('/notes/:id/ratings', require('./ratings'));
router.use('/notes', require('./notes'));
router.use('/search', require('./search'));
router.use('/admin', require('./admin'));
router.use('/profile', require('./profile'));
router.use('/share', require('./share'));

// Home page
router.get('/', (req, res) => {
  if (req.isAuthenticated()) return res.redirect('/notes');
  res.render('home', { title: 'Welcome to Loose Notes' });
});

// Top-rated notes
router.get('/top-rated', async (req, res, next) => {
  try {
    const notes = await Note.findAll({
      where: { visibility: 'public' },
      include: [
        { model: User, as: 'author', attributes: ['username'] },
        { model: Rating, attributes: ['value'] },
      ],
    });

    const withRatings = notes
      .filter((n) => n.Ratings.length >= 3)
      .map((n) => {
        const avg = n.Ratings.reduce((s, r) => s + r.value, 0) / n.Ratings.length;
        return {
          id: n.id,
          title: n.title,
          author: n.author ? n.author.username : 'Unknown',
          avgRating: avg.toFixed(1),
          ratingCount: n.Ratings.length,
          preview: n.content.slice(0, 200),
        };
      })
      .sort((a, b) => b.avgRating - a.avgRating)
      .slice(0, 20);

    res.render('topRated', { title: 'Top Rated Notes', notes: withRatings, csrfToken: req.csrfToken ? req.csrfToken() : '' });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
