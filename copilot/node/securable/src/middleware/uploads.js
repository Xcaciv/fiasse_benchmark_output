'use strict';

const path = require('node:path');

const multer = require('multer');

const { dataDirectories } = require('../database');
const { createRandomToken } = require('../utils/crypto');

const allowedExtensions = new Set(['.pdf', '.doc', '.docx', '.txt', '.png', '.jpg', '.jpeg']);
const maxFileSize = Number(process.env.MAX_UPLOAD_BYTES || 5 * 1024 * 1024);

const storage = multer.diskStorage({
  destination: (req, file, callback) => {
    callback(null, dataDirectories.uploads);
  },
  filename: (req, file, callback) => {
    const extension = path.extname(file.originalname).toLowerCase();
    callback(null, `${createRandomToken(16)}${extension}`);
  }
});

const upload = multer({
  storage,
  limits: {
    fileSize: maxFileSize,
    files: 5
  },
  fileFilter: (req, file, callback) => {
    const extension = path.extname(file.originalname).toLowerCase();
    if (!allowedExtensions.has(extension)) {
      return callback(new Error('Unsupported file type.'));
    }

    return callback(null, true);
  }
});

module.exports = {
  allowedExtensions,
  maxFileSize,
  upload
};
