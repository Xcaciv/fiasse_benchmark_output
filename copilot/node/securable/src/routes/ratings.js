'use strict';
const express = require('express');
const { body } = require('express-validator');
const { requireAuthenticated } = require('../middleware/requireAuth');
const ratingController = require('../controllers/ratingController');

const router = express.Router({ mergeParams: true });

router.post('/', requireAuthenticated,
  body('stars').isInt({ min: 1, max: 5 }).withMessage('Stars must be between 1 and 5'),
  body('comment').optional().trim().isLength({ max: 500 }).withMessage('Comment max 500 chars'),
  ratingController.addOrUpdateRating
);

module.exports = router;
