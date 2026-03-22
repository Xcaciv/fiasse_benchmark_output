'use strict';
const express = require('express');
const { body } = require('express-validator');
const { requireAuthenticated } = require('../middleware/requireAuth');
const profileController = require('../controllers/profileController');

const router = express.Router();

router.get('/', requireAuthenticated, profileController.showProfile);
router.post('/', requireAuthenticated,
  body('username').trim().isAlphanumeric().isLength({ min: 3, max: 30 }).withMessage('Username must be 3-30 alphanumeric characters'),
  body('email').trim().isEmail().withMessage('Valid email required'),
  body('newPassword').optional({ checkFalsy: true }).isLength({ min: 8 }).withMessage('New password must be at least 8 characters'),
  profileController.updateProfile
);

module.exports = router;
