'use strict';

const express = require('express');
const router = express.Router({ mergeParams: true });
const attachmentController = require('../controllers/attachmentController');
const requireAuth = require('../middleware/requireAuth');
const requireNoteOwner = require('../middleware/requireNoteOwner');
const { upload } = require('../config/upload');

router.get('/:attachId/download', attachmentController.downloadAttachment);

router.use(requireAuth);
router.post('/', requireNoteOwner(), upload.single('file'), attachmentController.postUploadAttachment);
router.delete('/:attachId', requireNoteOwner({ adminAllowed: true }), attachmentController.deleteAttachment);

module.exports = router;
