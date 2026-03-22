'use strict';
const express = require('express');
const passport = require('passport');
const { body } = require('express-validator');
const { authLimiter } = require('../middleware/rateLimiter');
const authController = require('../controllers/authController');

const router = express.Router();

router.get('/login', (req, res) => res.render('auth/login', { csrfToken: req.csrfToken(), errors: [] }));

router.post('/login', authLimiter,
  body('username').trim().notEmpty().withMessage('Username is required'),
  body('password').notEmpty().withMessage('Password is required'),
  passport.authenticate('local', {
    successRedirect: '/notes',
    failureRedirect: '/auth/login',
    failureFlash: true
  })
);

router.get('/register', (req, res) => res.render('auth/register', { csrfToken: req.csrfToken(), errors: [] }));

router.post('/register', authLimiter,
  body('username').trim().isAlphanumeric().isLength({ min: 3, max: 30 }).withMessage('Username must be 3-30 alphanumeric characters'),
  body('email').trim().isEmail().withMessage('Valid email required'),
  body('password').isLength({ min: 8 }).withMessage('Password must be at least 8 characters'),
  body('confirmPassword').custom((value, { req }) => {
    if (value !== req.body.password) throw new Error('Passwords do not match');
    return true;
  }),
  authController.register
);

router.post('/logout', authController.logout);

router.get('/forgot-password', (req, res) => res.render('auth/forgot-password', { csrfToken: req.csrfToken(), errors: [] }));
router.post('/forgot-password', authLimiter,
  body('email').trim().isEmail().withMessage('Valid email required'),
  authController.forgotPassword
);

router.get('/reset-password/:token', authController.showResetForm);
router.post('/reset-password/:token',
  body('password').isLength({ min: 8 }).withMessage('Password must be at least 8 characters'),
  body('confirmPassword').custom((value, { req }) => {
    if (value !== req.body.password) throw new Error('Passwords do not match');
    return true;
  }),
  authController.resetPassword
);

module.exports = router;
