'use strict';
const express = require('express');
const { requireAuthenticated } = require('../middleware/requireAuth');
const shareController = require('../controllers/shareController');

const router = express.Router();

router.post('/notes/:noteId/share', requireAuthenticated, shareController.generateShareLink);
router.post('/notes/:noteId/share/revoke', requireAuthenticated, shareController.revokeShareLink);
router.get('/share/:token', shareController.viewShared);

module.exports = router;
