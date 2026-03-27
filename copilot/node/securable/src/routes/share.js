'use strict';

const express = require('express');
const router = express.Router();
const shareController = require('../controllers/shareController');

router.get('/:token', shareController.getSharedNote);

module.exports = router;
