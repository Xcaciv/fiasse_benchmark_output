const express = require('express');
const router = express.Router();
const { Note, User, Rating } = require('../models');
const { Op, fn, col } = require('sequelize');

router.get('/', async (req, res) => {
  try {
    let recentNotes = [];
    if (req.isAuthenticated()) {
      recentNotes = await Note.findAll({
        where: {
          [Op.or]: [
            { userId: req.user.id },
            { visibility: 'public' },
          ],
        },
        include: [{ model: User, as: 'author', attributes: ['username'] }],
        order: [['createdAt', 'DESC']],
        limit: 6,
      });
    } else {
      recentNotes = await Note.findAll({
        where: { visibility: 'public' },
        include: [{ model: User, as: 'author', attributes: ['username'] }],
        order: [['createdAt', 'DESC']],
        limit: 6,
      });
    }
    res.render('index', { title: 'Loose Notes', notes: recentNotes, user: req.user || null, messages: req.flash() });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
