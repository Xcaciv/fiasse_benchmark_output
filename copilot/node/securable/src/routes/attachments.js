'use strict';
const express = require('express');
const path = require('path');
const multer = require('multer');
const { v4: uuidv4 } = require('uuid');
const { requireAuthenticated } = require('../middleware/requireAuth');
const { uploadLimiter } = require('../middleware/rateLimiter');
const attachmentController = require('../controllers/attachmentController');
const { MAX_FILE_SIZE_BYTES, ALLOWED_EXTENSIONS, ALLOWED_MIME_TYPES } = require('../config/constants');

const router = express.Router({ mergeParams: true });

// Integrity: Multer config with strict file filter — trust boundary for uploads
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, process.env.UPLOAD_DIR || './uploads'),
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    cb(null, `${uuidv4()}${ext}`);
  }
});

function fileFilter(req, file, cb) {
  const ext = path.extname(file.originalname).toLowerCase();
  if (ALLOWED_EXTENSIONS.includes(ext) && ALLOWED_MIME_TYPES.includes(file.mimetype)) {
    cb(null, true);
  } else {
    cb(new Error('File type not allowed'), false);
  }
}

const upload = multer({ storage, fileFilter, limits: { fileSize: MAX_FILE_SIZE_BYTES } });

router.post('/', requireAuthenticated, uploadLimiter, upload.single('attachment'), attachmentController.upload);

module.exports = router;
