'use strict';
const express = require('express');
const { query, body } = require('express-validator');
const { requireAuthenticated, requireAdmin } = require('../middleware/requireAuth');
const adminController = require('../controllers/adminController');

const router = express.Router();

router.use(requireAuthenticated, requireAdmin);

router.get('/', adminController.dashboard);
router.get('/users', adminController.listUsers);
router.get('/users/search', 
  query('q').trim().notEmpty().isLength({ max: 100 }),
  adminController.searchUsers
);
router.post('/notes/:noteId/reassign',
  body('newOwnerId').trim().notEmpty().withMessage('New owner ID required'),
  adminController.reassignNote
);

module.exports = router;
