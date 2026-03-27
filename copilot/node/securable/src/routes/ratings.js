'use strict';

const express = require('express');
const router = express.Router({ mergeParams: true });
const ratingController = require('../controllers/ratingController');
const requireAuth = require('../middleware/requireAuth');
const { ratingValidator } = require('../validators/ratingValidators');

router.get('/', requireAuth, ratingController.getRatings);
router.post('/', requireAuth, ratingValidator, ratingController.upsertRating);

module.exports = router;
