'use strict';

const express = require('express');
const authRouter = require('./auth');
const notesRouter = require('./notes');
const shareRouter = require('./share');
const profileRouter = require('./profile');
const adminRouter = require('./admin');

const router = express.Router();

// Root redirect
router.get('/', (req, res) => {
  if (req.session && req.session.userId) return res.redirect('/notes');
  res.redirect('/auth/login');
});

router.use('/auth', authRouter);
router.use('/notes', notesRouter);
router.use('/share', shareRouter);
router.use('/profile', profileRouter);
router.use('/admin', adminRouter);

module.exports = router;
