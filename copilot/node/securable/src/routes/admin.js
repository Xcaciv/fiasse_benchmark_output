'use strict';

const express = require('express');
const router = express.Router();
const adminController = require('../controllers/adminController');
const requireAuth = require('../middleware/requireAuth');
const requireAdmin = require('../middleware/requireAdmin');

router.use(requireAuth, requireAdmin);

router.get('/', adminController.getDashboard);
router.get('/users', adminController.getUsers);
router.get('/users/search', adminController.searchUsers);
router.post('/notes/:id/reassign', adminController.postReassignNote);

module.exports = router;
