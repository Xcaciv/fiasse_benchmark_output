const express = require('express');
const router = express.Router();
const db = require('../models');

router.get('/', async (req, res, next) => {
  try {
    let recentNotes = [];
    if (req.user) {
      recentNotes = await db.Note.findAll({
        where: { userId: req.user.id },
        order: [['createdAt', 'DESC']],
        limit: 5,
        include: [{ model: db.User, as: 'author', attributes: ['username'] }]
      });
    }
    res.render('index', { title: 'Home', recentNotes });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
