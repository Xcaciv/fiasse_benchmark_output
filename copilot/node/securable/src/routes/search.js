'use strict';

const express = require('express');
const router = express.Router();
const searchController = require('../controllers/searchController');
const requireAuth = require('../middleware/requireAuth');

router.get('/', requireAuth, searchController.getSearchResults);

module.exports = router;
