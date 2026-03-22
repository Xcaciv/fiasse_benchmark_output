'use strict';
const express = require('express');
const { query } = require('express-validator');
const searchController = require('../controllers/searchController');

const router = express.Router();

router.get('/', 
  query('q').trim().notEmpty().isLength({ max: 200 }).withMessage('Search query required (max 200 chars)'),
  searchController.search
);

module.exports = router;
