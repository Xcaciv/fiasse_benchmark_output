'use strict';

const express = require('express');
const router = express.Router();
const profileController = require('../controllers/profileController');
const requireAuth = require('../middleware/requireAuth');
const { registerValidator } = require('../validators/authValidators');

router.use(requireAuth);

router.get('/', profileController.getProfilePage);
router.put('/', registerValidator, profileController.putUpdateProfile);

module.exports = router;
