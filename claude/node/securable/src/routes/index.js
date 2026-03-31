'use strict';

const express = require('express');

const router = express.Router();

// GET / — home page
router.get('/', (req, res) => {
  if (req.isAuthenticated()) {
    return res.redirect('/notes');
  }
  res.render('home', { title: 'Loose Notes' });
});

module.exports = router;
