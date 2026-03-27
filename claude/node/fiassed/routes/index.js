'use strict';

const express = require('express');
const { softAuthenticate } = require('../middleware/authenticate');

const router = express.Router();

// Home page - publicly accessible
router.get('/', softAuthenticate, (req, res) => {
  res.render('index', {
    title: 'Loose Notes',
    currentUser: req.currentUser
  });
});

module.exports = router;
